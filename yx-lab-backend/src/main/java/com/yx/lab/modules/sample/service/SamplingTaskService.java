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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SamplingTaskService {

    private static final String TASK_SCOPE_TODO = "todo";

    private final SamplingTaskMapper samplingTaskMapper;

    private final LabSampleMapper labSampleMapper;

    private final SamplingPlanService samplingPlanService;

    private final StorageService storageService;

    private final DataScopeHelper dataScopeHelper;

    private final SampleNoGeneratorService sampleNoGeneratorService;

    public PageResult<SamplingTask> page(SamplingTaskQuery query) {
        if (isTodoScope(query)) {
            return pageTodoTasks(query);
        }
        Long scopedSamplerId = resolveScopedSamplerId(query.getSamplerId());
        LambdaQueryWrapper<SamplingTask> wrapper = new LambdaQueryWrapper<SamplingTask>()
                .and(StrUtil.isNotBlank(query.getKeyword()), item -> item
                        .like(SamplingTask::getTaskNo, query.getKeyword())
                        .or()
                        .like(SamplingTask::getPointName, query.getKeyword())
                        .or()
                        .like(SamplingTask::getSampleNo, query.getKeyword()))
                .eq(StrUtil.isNotBlank(query.getTaskStatus()), SamplingTask::getTaskStatus, query.getTaskStatus());
        applyTaskSamplerScope(wrapper, scopedSamplerId);
        wrapper.orderByDesc(SamplingTask::getCreatedTime);
        Page<SamplingTask> page = samplingTaskMapper.selectPage(PageUtils.buildPage(query), wrapper);
        page.getRecords().forEach(this::normalizeTaskFileUrlsForView);
        page.getRecords().forEach(this::enrichTaskDetectionConfigSnapshotForView);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public List<StatusCountVO> statusStats(SamplingTaskQuery query) {
        if (isTodoScope(query)) {
            return todoStatusStats(query);
        }
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

    public List<StatusCountVO> statusStats() {
        return statusStats(null);
    }

    private StatusCountVO statusCount(String status, Long count) {
        StatusCountVO vo = new StatusCountVO();
        vo.setStatus(status);
        vo.setCount(count == null ? 0L : count);
        return vo;
    }

    private Long countTasksByStatus(String taskStatus) {
        LambdaQueryWrapper<SamplingTask> wrapper = new LambdaQueryWrapper<SamplingTask>()
                .eq(StrUtil.isNotBlank(taskStatus), SamplingTask::getTaskStatus, taskStatus);
        applyTaskSamplerScope(wrapper, resolveScopedSamplerId(null));
        Number count = samplingTaskMapper.selectCount(wrapper);
        return count == null ? 0L : count.longValue();
    }

    private Long countUnloggedCompletedTasks() {
        LambdaQueryWrapper<SamplingTask> wrapper = new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getTaskStatus, LabWorkflowConstants.SamplingTaskStatus.COMPLETED);
        applyTaskSamplerScope(wrapper, resolveScopedSamplerId(null));
        List<SamplingTask> completedTasks = samplingTaskMapper.selectList(wrapper);
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

    private boolean isTodoScope(SamplingTaskQuery query) {
        return query != null && TASK_SCOPE_TODO.equalsIgnoreCase(StrUtil.trim(query.getScope()));
    }

    private PageResult<SamplingTask> pageTodoTasks(SamplingTaskQuery query) {
        List<SamplingTask> records = loadTodoTasks(query, false);
        long total = records.size();
        long pageNum = query == null || query.getPageNum() <= 0 ? 1L : query.getPageNum();
        long pageSize = query == null || query.getPageSize() <= 0 ? 10L : query.getPageSize();
        int fromIndex = (int) Math.min((pageNum - 1) * pageSize, total);
        int toIndex = (int) Math.min(fromIndex + pageSize, total);
        List<SamplingTask> pageRecords = total == 0L
                ? Collections.emptyList()
                : new ArrayList<>(records.subList(fromIndex, toIndex));
        pageRecords.forEach(this::normalizeTaskFileUrlsForView);
        pageRecords.forEach(this::enrichTaskDetectionConfigSnapshotForView);
        return new PageResult<>(total, pageRecords);
    }

    private List<StatusCountVO> todoStatusStats(SamplingTaskQuery query) {
        List<SamplingTask> records = loadTodoTasks(query, true);
        long pendingCount = records.stream()
                .filter(task -> LabWorkflowConstants.SamplingTaskStatus.PENDING.equals(task.getTaskStatus()))
                .count();
        long progressCount = records.stream()
                .filter(task -> LabWorkflowConstants.SamplingTaskStatus.IN_PROGRESS.equals(task.getTaskStatus()))
                .count();
        long unloggedCount = records.stream()
                .filter(task -> LabWorkflowConstants.SamplingTaskStatus.COMPLETED.equals(task.getTaskStatus()))
                .count();
        long todoCount = pendingCount + progressCount + unloggedCount;
        return Arrays.asList(
                statusCount("TODO", todoCount),
                statusCount("ALL", todoCount),
                statusCount(LabWorkflowConstants.SamplingTaskStatus.PENDING, pendingCount),
                statusCount(LabWorkflowConstants.SamplingTaskStatus.IN_PROGRESS, progressCount),
                statusCount("UNLOGGED", unloggedCount)
        );
    }

    private List<SamplingTask> loadTodoTasks(SamplingTaskQuery query, boolean ignoreTaskStatus) {
        String keyword = query == null ? null : StrUtil.trim(query.getKeyword());
        String taskStatus = query == null ? null : StrUtil.trim(query.getTaskStatus());
        Long scopedSamplerId = resolveScopedSamplerId(query == null ? null : query.getSamplerId());
        LambdaQueryWrapper<SamplingTask> wrapper = new LambdaQueryWrapper<SamplingTask>()
                .and(StrUtil.isNotBlank(keyword), item -> item
                        .like(SamplingTask::getTaskNo, keyword)
                        .or()
                        .like(SamplingTask::getPointName, keyword)
                        .or()
                        .like(SamplingTask::getSampleNo, keyword))
                .eq(!ignoreTaskStatus && StrUtil.isNotBlank(taskStatus), SamplingTask::getTaskStatus, taskStatus)
                .in(SamplingTask::getTaskStatus,
                        LabWorkflowConstants.SamplingTaskStatus.PENDING,
                        LabWorkflowConstants.SamplingTaskStatus.IN_PROGRESS,
                        LabWorkflowConstants.SamplingTaskStatus.COMPLETED)
                .orderByDesc(SamplingTask::getCreatedTime);
        applyTaskSamplerScope(wrapper, scopedSamplerId);
        List<SamplingTask> candidates = samplingTaskMapper.selectList(wrapper);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, LabSample> sampleMap = loadSampleMapByTaskIds(candidates);
        return candidates.stream()
                .filter(task -> shouldShowTodoTask(task, sampleMap.get(task.getId())))
                .collect(Collectors.toList());
    }

    private Map<Long, LabSample> loadSampleMapByTaskIds(List<SamplingTask> tasks) {
        List<Long> taskIds = tasks.stream()
                .map(SamplingTask::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return labSampleMapper.selectList(new LambdaQueryWrapper<LabSample>()
                        .in(LabSample::getTaskId, taskIds))
                .stream()
                .filter(sample -> sample.getTaskId() != null)
                .collect(Collectors.toMap(LabSample::getTaskId, sample -> sample, (left, right) -> left, LinkedHashMap::new));
    }

    private boolean shouldShowTodoTask(SamplingTask task, LabSample sample) {
        if (task == null) {
            return false;
        }
        if (LabWorkflowConstants.TODO_TASK_STATUSES.contains(task.getTaskStatus())) {
            return true;
        }
        return LabWorkflowConstants.SamplingTaskStatus.COMPLETED.equals(task.getTaskStatus()) && sample == null;
    }

    public SamplingTask detail(Long id) {
        SamplingTask task = requireTask(id);
        normalizeTaskFileUrlsForView(task);
        enrichTaskDetectionConfigSnapshotForView(task);
        return task;
    }

    public List<SamplingTask> todoMine() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        LambdaQueryWrapper<SamplingTask> wrapper = new LambdaQueryWrapper<SamplingTask>()
                .in(SamplingTask::getTaskStatus, LabWorkflowConstants.TODO_TASK_STATUSES);
        applyTaskSamplerScope(wrapper, currentUser.getUserId());
        wrapper.orderByAsc(SamplingTask::getSamplingTime);
        List<SamplingTask> tasks = samplingTaskMapper.selectList(wrapper);
        tasks.forEach(this::normalizeTaskFileUrlsForView);
        tasks.forEach(this::enrichTaskDetectionConfigSnapshotForView);
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
        task.setOnsiteMetrics(null);
        task.setWeather(command.getWeather());
        task.setTemperature(command.getTemperature());
        task.setSampleTotalVolume(StrUtil.trim(command.getSampleTotalVolume()));
        task.setSampleBottleCount(StrUtil.trim(command.getSampleBottleCount()));
        task.setPhotoUrls(normalizePhotoUrls(command.getPhotoUrls()));
        task.setRemark(command.getRemark());
        if (StrUtil.isNotBlank(command.getAddress())) {
            task.setAddress(StrUtil.trim(command.getAddress()));
        }
        if (StrUtil.isNotBlank(command.getLatitude())) {
            task.setLatitude(StrUtil.trim(command.getLatitude()));
        }
        if (StrUtil.isNotBlank(command.getLongitude())) {
            task.setLongitude(StrUtil.trim(command.getLongitude()));
        }
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

    private void enrichTaskDetectionConfigSnapshotForView(SamplingTask task) {
        if (task != null) {
            task.setDetectionConfigSnapshot(samplingPlanService.enrichDetectionConfigSnapshotForView(task.getDetectionConfigSnapshot()));
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
        if (!isSamplerAssigned(task, currentUser.getUserId())) {
            throw new BusinessException("当前用户不是该采样任务的责任采样员，不能执行此操作。");
        }
    }

    private void applyTaskSamplerScope(LambdaQueryWrapper<SamplingTask> wrapper, Long samplerId) {
        if (wrapper == null || samplerId == null) {
            return;
        }
        wrapper.and(item -> item
                .eq(SamplingTask::getSamplerId, samplerId)
                .or()
                .like(SamplingTask::getSamplerIds, wrapSamplerId(samplerId)));
    }

    private boolean isSamplerAssigned(SamplingTask task, Long samplerId) {
        if (task == null || samplerId == null) {
            return false;
        }
        if (samplerId.equals(task.getSamplerId())) {
            return true;
        }
        return StrUtil.contains(task.getSamplerIds(), wrapSamplerId(samplerId));
    }

    private String wrapSamplerId(Long samplerId) {
        return samplerId == null ? null : "," + samplerId + ",";
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
        Number sampleCount = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()
                .eq(LabSample::getTaskId, task.getId()));
        return sampleCount != null && sampleCount.longValue() > 0;
    }
}
