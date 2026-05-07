package com.yx.lab.modules.sample.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.sample.dto.SamplingPlanDispatchCommand;
import com.yx.lab.modules.sample.dto.SamplingPlanQuery;
import com.yx.lab.modules.sample.dto.SamplingPlanSaveCommand;
import com.yx.lab.modules.sample.entity.SamplingPlan;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.mapper.SamplingPlanMapper;
import com.yx.lab.modules.sample.mapper.SamplingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SamplingPlanService {

    private final SamplingPlanMapper samplingPlanMapper;

    private final SamplingTaskMapper samplingTaskMapper;

    public PageResult<SamplingPlan> page(SamplingPlanQuery query) {
        Page<SamplingPlan> page = samplingPlanMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<SamplingPlan>()
                        .like(StrUtil.isNotBlank(query.getKeyword()), SamplingPlan::getPlanName, query.getKeyword())
                        .eq(StrUtil.isNotBlank(query.getPlanStatus()), SamplingPlan::getPlanStatus, query.getPlanStatus())
                        .orderByDesc(SamplingPlan::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public SamplingPlan detail(Long id) {
        return requirePlan(id);
    }

    public void save(SamplingPlanSaveCommand command) {
        SamplingPlan plan = new SamplingPlan();
        applyPlanCommand(plan, command);
        if (StrUtil.isBlank(plan.getPlanStatus())) {
            plan.setPlanStatus(LabWorkflowConstants.SamplingPlanStatus.ACTIVE);
        }
        if (StrUtil.isBlank(plan.getCycleType())) {
            plan.setCycleType(LabWorkflowConstants.CycleType.ONCE);
        }
        samplingPlanMapper.insert(plan);
    }

    public void update(Long id, SamplingPlanSaveCommand command) {
        SamplingPlan existing = requirePlan(id);
        if (isLockedPlan(existing.getPlanStatus())) {
            throw new BusinessException("当前计划已进入执行阶段，不允许直接编辑");
        }
        String originalPlanStatus = existing.getPlanStatus();
        String originalCycleType = existing.getCycleType();
        applyPlanCommand(existing, command);
        if (StrUtil.isBlank(existing.getPlanStatus())) {
            existing.setPlanStatus(originalPlanStatus);
        }
        if (StrUtil.isBlank(existing.getCycleType())) {
            existing.setCycleType(originalCycleType);
        }
        samplingPlanMapper.updateById(existing);
    }

    public void delete(Long id) {
        SamplingPlan existing = requirePlan(id);
        if (isLockedPlan(existing.getPlanStatus())) {
            throw new BusinessException("当前计划已执行，不允许删除");
        }
        samplingPlanMapper.deleteById(id);
    }

    public void pause(Long id) {
        SamplingPlan plan = requirePlan(id);
        if (!LabWorkflowConstants.SamplingPlanStatus.ACTIVE.equals(plan.getPlanStatus())
                && !LabWorkflowConstants.SamplingPlanStatus.UNPUBLISHED.equals(plan.getPlanStatus())) {
            throw new BusinessException("当前计划状态不允许暂停");
        }
        plan.setPlanStatus(LabWorkflowConstants.SamplingPlanStatus.PAUSED);
        samplingPlanMapper.updateById(plan);
    }

    public void resume(Long id) {
        SamplingPlan plan = requirePlan(id);
        if (!LabWorkflowConstants.SamplingPlanStatus.PAUSED.equals(plan.getPlanStatus())) {
            throw new BusinessException("当前计划不处于暂停状态");
        }
        plan.setPlanStatus(LabWorkflowConstants.SamplingPlanStatus.ACTIVE);
        samplingPlanMapper.updateById(plan);
    }

    @Transactional(rollbackFor = Exception.class)
    public void dispatch(SamplingPlanDispatchCommand command) {
        SamplingPlan plan = requirePlan(command.getPlanId());
        if (!LabWorkflowConstants.DISPATCHABLE_PLAN_STATUSES.contains(plan.getPlanStatus())) {
            throw new BusinessException("当前计划状态不允许派发");
        }

        SamplingTask task = new SamplingTask();
        task.setPlanId(plan.getId());
        task.setPointId(plan.getPointId());
        task.setPointName(plan.getPointName());
        task.setSamplingTime(command.getSamplingTime() == null ? plan.getStartTime() : command.getSamplingTime());
        task.setSamplerId(plan.getSamplerId());
        task.setSamplerName(plan.getSamplerName());
        task.setSampleType(plan.getSampleType());
        task.setTaskStatus(LabWorkflowConstants.SamplingTaskStatus.PENDING);
        task.setRemark("由采样计划派发生成");
        samplingTaskMapper.insert(task);

        plan.setPlanStatus(LabWorkflowConstants.SamplingPlanStatus.DISPATCHED);
        plan.setUpdatedTime(LocalDateTime.now());
        samplingPlanMapper.updateById(plan);
    }

    private SamplingPlan requirePlan(Long id) {
        SamplingPlan plan = samplingPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("采样计划不存在");
        }
        return plan;
    }

    private boolean isLockedPlan(String planStatus) {
        return LabWorkflowConstants.SamplingPlanStatus.DISPATCHED.equals(planStatus)
                || LabWorkflowConstants.SamplingPlanStatus.COMPLETED.equals(planStatus);
    }

    private void applyPlanCommand(SamplingPlan plan, SamplingPlanSaveCommand command) {
        plan.setPlanName(StrUtil.trim(command.getPlanName()));
        plan.setPointId(command.getPointId());
        plan.setPointName(StrUtil.trim(command.getPointName()));
        plan.setStartTime(command.getStartTime());
        plan.setEndTime(command.getEndTime());
        plan.setSamplerId(command.getSamplerId());
        plan.setSamplerName(StrUtil.trim(command.getSamplerName()));
        plan.setSamplingType(StrUtil.trim(command.getSamplingType()));
        plan.setSampleType(StrUtil.trim(command.getSampleType()));
        plan.setCycleType(StrUtil.trim(command.getCycleType()));
        plan.setPlanStatus(StrUtil.trim(command.getPlanStatus()));
        plan.setRemark(StrUtil.trim(command.getRemark()));
    }
}
