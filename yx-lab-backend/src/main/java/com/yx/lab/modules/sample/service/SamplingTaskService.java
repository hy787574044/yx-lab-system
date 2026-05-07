package com.yx.lab.modules.sample.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.sample.dto.SamplingTaskActionCommand;
import com.yx.lab.modules.sample.dto.SamplingTaskCompleteCommand;
import com.yx.lab.modules.sample.dto.SamplingTaskQuery;
import com.yx.lab.modules.sample.entity.SamplingPlan;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.mapper.SamplingPlanMapper;
import com.yx.lab.modules.sample.mapper.SamplingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SamplingTaskService {

    private final SamplingTaskMapper samplingTaskMapper;

    private final SamplingPlanMapper samplingPlanMapper;

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

    public SamplingTask detail(Long id) {
        return requireTask(id);
    }

    public List<SamplingTask> todoMine() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        return samplingTaskMapper.selectList(new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getSamplerId, currentUser.getUserId())
                .in(SamplingTask::getTaskStatus, "PENDING", "IN_PROGRESS")
                .orderByAsc(SamplingTask::getSamplingTime));
    }

    public void start(Long taskId, SamplingTaskActionCommand command) {
        SamplingTask task = requireTask(taskId);
        if (!"PENDING".equals(task.getTaskStatus())) {
            throw new BusinessException("当前任务状态不允许开始执行");
        }
        task.setTaskStatus("IN_PROGRESS");
        task.setStartedTime(LocalDateTime.now());
        if (command != null && StrUtil.isNotBlank(command.getRemark())) {
            task.setRemark(command.getRemark());
        }
        samplingTaskMapper.updateById(task);
    }

    public void abandon(Long taskId, SamplingTaskActionCommand command) {
        SamplingTask task = requireTask(taskId);
        if (!"PENDING".equals(task.getTaskStatus()) && !"IN_PROGRESS".equals(task.getTaskStatus())) {
            throw new BusinessException("当前任务状态不允许废弃");
        }
        String reason = command == null ? null : StrUtil.trim(command.getReason());
        if (StrUtil.isBlank(reason)) {
            throw new BusinessException("废弃任务时必须填写原因");
        }
        task.setTaskStatus("ABANDONED");
        task.setAbandonReason(reason);
        if (command != null && StrUtil.isNotBlank(command.getRemark())) {
            task.setRemark(command.getRemark());
        }
        samplingTaskMapper.updateById(task);
    }

    public void resume(Long taskId, SamplingTaskActionCommand command) {
        SamplingTask task = requireTask(taskId);
        if (!"ABANDONED".equals(task.getTaskStatus())) {
            throw new BusinessException("当前任务不处于废弃状态");
        }
        task.setTaskStatus("PENDING");
        task.setAbandonReason(null);
        if (command != null && StrUtil.isNotBlank(command.getRemark())) {
            task.setRemark(command.getRemark());
        }
        samplingTaskMapper.updateById(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(SamplingTaskCompleteCommand command) {
        SamplingTask task = requireTask(command.getTaskId());
        if ("ABANDONED".equals(task.getTaskStatus())) {
            throw new BusinessException("已废弃的任务不能直接完成");
        }
        if ("COMPLETED".equals(task.getTaskStatus())) {
            throw new BusinessException("当前任务已完成");
        }
        if ("PENDING".equals(task.getTaskStatus())) {
            task.setStartedTime(LocalDateTime.now());
        }
        task.setOnsiteMetrics(command.getOnsiteMetrics());
        task.setPhotoUrls(command.getPhotoUrls());
        task.setRemark(command.getRemark());
        task.setTaskStatus("COMPLETED");
        task.setFinishedTime(LocalDateTime.now());
        samplingTaskMapper.updateById(task);

        if (task.getPlanId() != null) {
            SamplingPlan plan = samplingPlanMapper.selectById(task.getPlanId());
            if (plan != null) {
                plan.setPlanStatus("COMPLETED");
                samplingPlanMapper.updateById(plan);
            }
        }
    }

    private SamplingTask requireTask(Long id) {
        SamplingTask task = samplingTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("采样任务不存在");
        }
        return task;
    }
}
