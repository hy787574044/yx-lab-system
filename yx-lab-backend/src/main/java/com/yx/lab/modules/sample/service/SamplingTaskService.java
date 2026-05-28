package com.yx.lab.modules.sample.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.DataScopeHelper;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.sample.dto.SamplingTaskActionCommand;
import com.yx.lab.modules.sample.dto.SamplingTaskCompleteCommand;
import com.yx.lab.modules.sample.dto.SamplingTaskQuery;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.sample.mapper.SamplingTaskMapper;
import com.yx.lab.modules.sample.vo.StatusCountVO;
import com.yx.lab.modules.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SamplingTaskService {

    private final SamplingTaskMapper samplingTaskMapper;

    private final LabSampleMapper labSampleMapper;

    private final SamplingPlanService samplingPlanService;

    private final StorageService storageService;

    private final DataScopeHelper dataScopeHelper;

    private final SampleNoGeneratorService sampleNoGeneratorService;

    public PageResult<SamplingTask> page(SamplingTaskQuery query) {
        Page<SamplingTask> page = samplingTaskMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<SamplingTask>()
                        .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                                .like(SamplingTask::getTaskNo, query.getKeyword())
                                .or()
                                .like(SamplingTask::getPointName, query.getKeyword())
                                .or()
                                .like(SamplingTask::getSampleNo, query.getKeyword()))
                        .eq(StrUtil.isNotBlank(query.getTaskStatus()), SamplingTask::getTaskStatus, query.getTaskStatus())
                        .eq(resolveScopedSamplerId(query.getSamplerId()) != null,
                                SamplingTask::getSamplerId,
                                resolveScopedSamplerId(query.getSamplerId()))
                        .orderByDesc(SamplingTask::getCreatedTime));
        page.getRecords().forEach(this::normalizeTaskFileUrlsForView);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public List<StatusCountVO> statusStats() {
        return Arrays.asList(
                statusCount("ALL", countTasksByStatus(null)),
                statusCount(LabWorkflowConstants.SamplingTaskStatus.PENDING,
                        countTasksByStatus(LabWorkflowConstants.SamplingTaskStatus.PENDING)),
                statusCount(LabWorkflowConstants.SamplingTaskStatus.IN_PROGRESS,
                        countTasksByStatus(LabWorkflowConstants.SamplingTaskStatus.IN_PROGRESS)),
                statusCount(LabWorkflowConstants.SamplingTaskStatus.COMPLETED,
                        countTasksByStatus(LabWorkflowConstants.SamplingTaskStatus.COMPLETED)),
                statusCount(LabWorkflowConstants.SamplingTaskStatus.ABANDONED,
                        countTasksByStatus(LabWorkflowConstants.SamplingTaskStatus.ABANDONED)),
                statusCount("UNLOGGED", countUnloggedCompletedTasks()));
    }

    private StatusCountVO statusCount(String status, Long count) {
        StatusCountVO vo = new StatusCountVO();
        vo.setStatus(status);
        vo.setCount(count == null ? 0L : count);
        return vo;
    }

    private Long countTasksByStatus(String taskStatus) {
        return samplingTaskMapper.selectCount(new LambdaQueryWrapper<SamplingTask>()
                .eq(StrUtil.isNotBlank(taskStatus), SamplingTask::getTaskStatus, taskStatus)
                .eq(resolveScopedSamplerId(null) != null,
                        SamplingTask::getSamplerId,
                        resolveScopedSamplerId(null)));
    }

    private Long countUnloggedCompletedTasks() {
        List<SamplingTask> completedTasks = samplingTaskMapper.selectList(new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getTaskStatus, LabWorkflowConstants.SamplingTaskStatus.COMPLETED)
                .eq(resolveScopedSamplerId(null) != null,
                        SamplingTask::getSamplerId,
                        resolveScopedSamplerId(null)));
        return completedTasks.stream()
                .filter(task -> !isTaskRegistered(task))
                .count();
    }

    private Long resolveScopedSamplerId(Long querySamplerId) {
        if (dataScopeHelper.isAdmin()) {
            return querySamplerId;
        }
        if (dataScopeHelper.isRole("SAMPLER") && dataScopeHelper.currentUserId() != null) {
            return dataScopeHelper.currentUserId();
        }
        return querySamplerId;
    }

    public SamplingTask detail(Long id) {
        SamplingTask task = requireTask(id);
        normalizeTaskFileUrlsForView(task);
        return task;
    }

    public List<SamplingTask> todoMine() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        List<SamplingTask> tasks = samplingTaskMapper.selectList(new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getSamplerId, currentUser.getUserId())
                .in(SamplingTask::getTaskStatus, LabWorkflowConstants.TODO_TASK_STATUSES)
                .orderByAsc(SamplingTask::getSamplingTime));
        tasks.forEach(this::normalizeTaskFileUrlsForView);
        return tasks;
    }

    public void start(Long taskId, SamplingTaskActionCommand command) {
        SamplingTask task = requireTask(taskId);
        validateTaskOperator(task);
        if (!LabWorkflowConstants.canStartTask(task.getTaskStatus())) {
            throw new BusinessException("当前任务状态不允许开始执行。");
        }
        task.setTaskStatus(LabWorkflowConstants.SamplingTaskStatus.IN_PROGRESS);
        task.setStartedTime(LocalDateTime.now());
        if (command != null && StrUtil.isNotBlank(command.getRemark())) {
            task.setRemark(command.getRemark());
        }
        samplingTaskMapper.updateById(task);
        samplingPlanService.refreshPlanStatusAfterTaskChange(task.getPlanId());
    }

    public void abandon(Long taskId, SamplingTaskActionCommand command) {
        SamplingTask task = requireTask(taskId);
        validateTaskOperator(task);
        if (!LabWorkflowConstants.canAbandonTask(task.getTaskStatus())) {
            throw new BusinessException("当前任务状态不允许废弃。");
        }
        String reason = command == null ? null : StrUtil.trim(command.getReason());
        if (StrUtil.isBlank(reason)) {
            throw new BusinessException("废弃任务时必须填写原因。");
        }
        task.setTaskStatus(LabWorkflowConstants.SamplingTaskStatus.ABANDONED);
        task.setAbandonReason(reason);
        if (command != null && StrUtil.isNotBlank(command.getRemark())) {
            task.setRemark(command.getRemark());
        }
        samplingTaskMapper.updateById(task);
        samplingPlanService.refreshPlanStatusAfterTaskChange(task.getPlanId());
    }

    public void resume(Long taskId, SamplingTaskActionCommand command) {
        SamplingTask task = requireTask(taskId);
        validateTaskOperator(task);
        if (!LabWorkflowConstants.canResumeTask(task.getTaskStatus())) {
            throw new BusinessException("当前任务不是废弃状态，不能恢复。");
        }
        task.setTaskStatus(LabWorkflowConstants.SamplingTaskStatus.PENDING);
        task.setAbandonReason(null);
        if (command != null && StrUtil.isNotBlank(command.getRemark())) {
            task.setRemark(command.getRemark());
        }
        samplingTaskMapper.updateById(task);
        samplingPlanService.refreshPlanStatusAfterTaskChange(task.getPlanId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(SamplingTaskCompleteCommand command) {
        SamplingTask task = requireTask(command.getTaskId());
        validateTaskOperator(task);
        if (LabWorkflowConstants.SamplingTaskStatus.ABANDONED.equals(task.getTaskStatus())) {
            throw new BusinessException("已废弃的任务不能直接完成。");
        }
        if (LabWorkflowConstants.SamplingTaskStatus.COMPLETED.equals(task.getTaskStatus())) {
            throw new BusinessException("当前任务已经完成。");
        }
        if (!LabWorkflowConstants.canCompleteTask(task.getTaskStatus())) {
            throw new BusinessException("请先开始采样任务，再提交完成。");
        }

        task.setSampleNo(sampleNoGeneratorService.ensureSampleNo(task.getSampleNo()));
        task.setOnsiteMetrics(command.getOnsiteMetrics());
        task.setWeather(command.getWeather());
        task.setTemperature(command.getTemperature());
        task.setPhotoUrls(normalizePhotoUrls(command.getPhotoUrls()));
        task.setRemark(command.getRemark());
        task.setAddress(command.getAddress());
        task.setLatitude(command.getLatitude());
        task.setLongitude(command.getLongitude());
        if (task.getStartedTime() == null) {
            task.setStartedTime(LocalDateTime.now());
        }
        task.setTaskStatus(LabWorkflowConstants.SamplingTaskStatus.COMPLETED);
        task.setFinishedTime(LocalDateTime.now());
        samplingTaskMapper.updateById(task);

        samplingPlanService.refreshPlanStatusAfterTaskChange(task.getPlanId());
    }

    private String normalizePhotoUrls(String photoUrls) {
        if (StrUtil.isBlank(photoUrls)) {
            return null;
        }
        return Arrays.stream(photoUrls.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(storageService::toFullUrl)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private void normalizeTaskFileUrlsForView(SamplingTask task) {
        if (task != null) {
            task.setPhotoUrls(storageService.toFullUrls(task.getPhotoUrls()));
        }
    }

    private SamplingTask requireTask(Long id) {
        SamplingTask task = samplingTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("采样任务不存在。");
        }
        return task;
    }

    private void validateTaskOperator(SamplingTask task) {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("当前登录信息已失效，请重新登录。");
        }
        if (isAdmin(currentUser)) {
            return;
        }
        if (task.getSamplerId() == null || !task.getSamplerId().equals(currentUser.getUserId())) {
            throw new BusinessException("当前用户不是该采样任务的责任采样员，不能执行此操作。");
        }
    }

    private boolean isAdmin(CurrentUser currentUser) {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRoleCode());
    }

    private boolean isTaskRegistered(SamplingTask task) {
        if (task == null) {
            return false;
        }
        if (task.getSampleId() != null
                || LabWorkflowConstants.SampleRegisterStatus.REGISTERED.equals(task.getSampleRegisterStatus())) {
            return true;
        }
        Long sampleCount = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()
                .eq(LabSample::getTaskId, task.getId()));
        return sampleCount != null && sampleCount > 0;
    }
}
