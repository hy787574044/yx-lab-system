package com.yx.lab.modules.sample.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.sample.dto.SamplingTaskActionCommand;
import com.yx.lab.modules.sample.dto.SamplingTaskCompleteCommand;
import com.yx.lab.modules.sample.dto.SamplingTaskQuery;
import com.yx.lab.modules.sample.dto.SamplingTaskSealNoCommand;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.sample.mapper.SamplingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 采样任务服务，负责现场采样任务的执行、封签号维护和状态流转。
 */
@Service
@RequiredArgsConstructor
public class SamplingTaskService {

    private final SamplingTaskMapper samplingTaskMapper;

    private final LabSampleMapper labSampleMapper;

    private final SamplingPlanService samplingPlanService;

    /**
     * 分页查询采样任务列表。
     *
     * @param query 查询条件
     * @return 采样任务分页结果
     */
    public PageResult<SamplingTask> page(SamplingTaskQuery query) {
        Page<SamplingTask> page = samplingTaskMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<SamplingTask>()
                        .like(StrUtil.isNotBlank(query.getKeyword()), SamplingTask::getPointName, query.getKeyword())
                        .eq(StrUtil.isNotBlank(query.getTaskStatus()), SamplingTask::getTaskStatus, query.getTaskStatus())
                        .eq(query.getSamplerId() != null, SamplingTask::getSamplerId, query.getSamplerId())
                        .orderByDesc(SamplingTask::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    /**
     * 获取采样任务详情。
     *
     * @param id 任务ID
     * @return 采样任务详情
     */
    public SamplingTask detail(Long id) {
        return requireTask(id);
    }

    /**
     * 查询当前登录采样员的待办任务。
     *
     * @return 我的采样任务列表
     */
    public List<SamplingTask> todoMine() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        return samplingTaskMapper.selectList(new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getSamplerId, currentUser.getUserId())
                .in(SamplingTask::getTaskStatus, LabWorkflowConstants.TODO_TASK_STATUSES)
                .orderByAsc(SamplingTask::getSamplingTime));
    }

    /**
     * 开始执行采样任务。
     *
     * @param taskId 任务ID
     * @param command 操作参数
     */
    public void start(Long taskId, SamplingTaskActionCommand command) {
        SamplingTask task = requireTask(taskId);
        validateTaskOperator(task);
        if (!LabWorkflowConstants.canStartTask(task.getTaskStatus())) {
            throw new BusinessException("当前任务状态不允许开始执行。");
        }

        // 允许在开始采样前补录封签号，但一旦样品已登记则不再允许修改。
        if (command != null && StrUtil.isNotBlank(command.getSealNo())) {
            validateSealNoEditable(task);
            applySealNo(task, command.getSealNo());
        }

        // 采样任务开始的硬门禁：封签号必须已存在，确保后续样品登录可准确关联。
        if (StrUtil.isBlank(task.getSealNo())) {
            throw new BusinessException("开始采样任务前必须先录入封签号。");
        }

        task.setTaskStatus(LabWorkflowConstants.SamplingTaskStatus.IN_PROGRESS);
        task.setStartedTime(LocalDateTime.now());
        if (command != null && StrUtil.isNotBlank(command.getRemark())) {
            task.setRemark(command.getRemark());
        }
        samplingTaskMapper.updateById(task);
        samplingPlanService.refreshPlanStatusAfterTaskChange(task.getPlanId());
    }

    /**
     * 维护采样任务封签号。
     *
     * @param taskId 任务ID
     * @param command 封签号参数
     */
    public void updateSealNo(Long taskId, SamplingTaskSealNoCommand command) {
        SamplingTask task = requireTask(taskId);
        validateTaskOperator(task);
        validateSealNoEditable(task);
        applySealNo(task, command.getSealNo());
        samplingTaskMapper.updateById(task);
    }

    /**
     * 作废采样任务。
     *
     * @param taskId 任务ID
     * @param command 操作参数
     */
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

    /**
     * 恢复已中止的采样任务。
     *
     * @param taskId 任务ID
     * @param command 操作参数
     */
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

    /**
     * 完成采样任务，并回填封签、任务状态及计划状态。
     *
     * @param command 完成任务参数
     */
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

        // 完成节点只沉淀现场采样结果与附件，样品主档在后续样品登录环节创建。
        task.setOnsiteMetrics(command.getOnsiteMetrics());
        task.setPhotoUrls(command.getPhotoUrls());
        task.setRemark(command.getRemark());
        task.setTaskStatus(LabWorkflowConstants.SamplingTaskStatus.COMPLETED);
        task.setFinishedTime(LocalDateTime.now());
        samplingTaskMapper.updateById(task);

        samplingPlanService.refreshPlanStatusAfterTaskChange(task.getPlanId());
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

    private void validateSealNoEditable(SamplingTask task) {
        if (task == null) {
            return;
        }
        if (task.getSampleId() != null
                || LabWorkflowConstants.SampleRegisterStatus.REGISTERED.equals(task.getSampleRegisterStatus())) {
            throw new BusinessException("样品已完成登记，不能再修改任务封签号。");
        }
    }

    private void applySealNo(SamplingTask task, String sealNo) {
        String normalizedSealNo = normalizeSealNo(sealNo);
        if (StrUtil.isBlank(normalizedSealNo)) {
            throw new BusinessException("封签号不能为空。");
        }

        // 封签号在采样任务和样品两个维度都必须唯一，避免一号多样或一号多任务。
        Long taskCount = samplingTaskMapper.selectCount(new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getSealNo, normalizedSealNo)
                .ne(task.getId() != null, SamplingTask::getId, task.getId()));
        if (taskCount != null && taskCount > 0) {
            throw new BusinessException("封签号已被其他采样任务占用，请核对后重试。");
        }

        Long sampleCount = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()
                .eq(LabSample::getSealNo, normalizedSealNo));
        if (sampleCount != null && sampleCount > 0) {
            throw new BusinessException("封签号已被样品占用，请核对后重试。");
        }

        task.setSealNo(normalizedSealNo);
    }

    private String normalizeSealNo(String sealNo) {
        return StrUtil.blankToDefault(StrUtil.trim(sealNo), null);
    }
}
