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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
        validatePlan(plan);
        samplingPlanMapper.insert(plan);
    }

    public void update(Long id, SamplingPlanSaveCommand command) {
        SamplingPlan existing = requirePlan(id);
        if (LabWorkflowConstants.isLockedPlan(existing.getPlanStatus())) {
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
        validatePlan(existing);
        samplingPlanMapper.updateById(existing);
    }

    public void delete(Long id) {
        SamplingPlan existing = requirePlan(id);
        if (LabWorkflowConstants.isLockedPlan(existing.getPlanStatus())) {
            throw new BusinessException("当前计划已执行，不允许删除");
        }
        samplingPlanMapper.deleteById(id);
    }

    public void pause(Long id) {
        SamplingPlan plan = requirePlan(id);
        if (!LabWorkflowConstants.canPausePlan(plan.getPlanStatus())) {
            throw new BusinessException("当前计划状态不允许暂停");
        }
        plan.setPlanStatus(LabWorkflowConstants.SamplingPlanStatus.PAUSED);
        samplingPlanMapper.updateById(plan);
    }

    public void resume(Long id) {
        SamplingPlan plan = requirePlan(id);
        if (!LabWorkflowConstants.canResumePlan(plan.getPlanStatus())) {
            throw new BusinessException("当前计划不处于暂停状态");
        }
        plan.setPlanStatus(LabWorkflowConstants.SamplingPlanStatus.ACTIVE);
        samplingPlanMapper.updateById(plan);
    }

    @Transactional(rollbackFor = Exception.class)
    public void dispatch(SamplingPlanDispatchCommand command) {
        SamplingPlan plan = requirePlan(command.getPlanId());
        if (!LabWorkflowConstants.canDispatchPlan(plan.getPlanStatus())) {
            throw new BusinessException("当前计划状态不允许派发");
        }
        dispatchPlanTask(plan, command.getSamplingTime(), true);
    }

    @Transactional(rollbackFor = Exception.class)
    public int autoDispatchDuePlans(LocalDateTime now) {
        LocalDateTime dispatchTime = now == null ? LocalDateTime.now() : now;
        int dispatchedCount = 0;
        for (SamplingPlan plan : samplingPlanMapper.selectList(new LambdaQueryWrapper<SamplingPlan>()
                .eq(SamplingPlan::getPlanStatus, LabWorkflowConstants.SamplingPlanStatus.ACTIVE)
                .in(SamplingPlan::getCycleType,
                        LabWorkflowConstants.CycleType.ONCE,
                        LabWorkflowConstants.CycleType.DAILY,
                        LabWorkflowConstants.CycleType.WEEKLY,
                        LabWorkflowConstants.CycleType.MONTHLY)
                .orderByAsc(SamplingPlan::getStartTime))) {
            LocalDateTime scheduledTime = resolveScheduledTime(plan, dispatchTime);
            if (scheduledTime == null) {
                continue;
            }
            if (dispatchPlanTask(plan, scheduledTime, false)) {
                dispatchedCount++;
            }
        }
        return dispatchedCount;
    }

    public void refreshPlanStatusAfterTaskCompletion(Long planId) {
        if (planId == null) {
            return;
        }
        SamplingPlan plan = samplingPlanMapper.selectById(planId);
        if (plan == null || LabWorkflowConstants.SamplingPlanStatus.PAUSED.equals(plan.getPlanStatus())) {
            return;
        }
        if (LabWorkflowConstants.isOnceCycle(plan.getCycleType())) {
            updatePlanStatus(plan, LabWorkflowConstants.SamplingPlanStatus.COMPLETED);
            return;
        }
        if (LabWorkflowConstants.isRecurringCycle(plan.getCycleType())
                && plan.getEndTime() != null
                && !plan.getEndTime().isAfter(LocalDateTime.now())) {
            updatePlanStatus(plan, LabWorkflowConstants.SamplingPlanStatus.COMPLETED);
            return;
        }
        if (!LabWorkflowConstants.SamplingPlanStatus.ACTIVE.equals(plan.getPlanStatus())) {
            updatePlanStatus(plan, LabWorkflowConstants.SamplingPlanStatus.ACTIVE);
        }
    }

    public SamplingPlan requirePlan(Long id) {
        SamplingPlan plan = samplingPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("采样计划不存在");
        }
        return plan;
    }

    private boolean dispatchPlanTask(SamplingPlan plan, LocalDateTime samplingTime, boolean manualDispatch) {
        LocalDateTime taskTime = samplingTime == null ? plan.getStartTime() : samplingTime;
        if (taskTime == null) {
            throw new BusinessException("采样计划未设置开始时间，不能派发任务");
        }
        if (plan.getEndTime() != null && taskTime.isAfter(plan.getEndTime())) {
            if (manualDispatch) {
                throw new BusinessException("任务执行时间已超出计划截止时间");
            }
            markPlanCompletedIfExpired(plan, taskTime);
            return false;
        }
        Long existingCount = samplingTaskMapper.selectCount(new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getPlanId, plan.getId())
                .eq(SamplingTask::getSamplingTime, taskTime));
        if (existingCount != null && existingCount > 0) {
            if (manualDispatch) {
                throw new BusinessException("当前计划在该执行时间已生成采样任务，不能重复派发");
            }
            return false;
        }
        SamplingTask task = new SamplingTask();
        task.setPlanId(plan.getId());
        task.setPointId(plan.getPointId());
        task.setPointName(plan.getPointName());
        task.setSamplingTime(taskTime);
        task.setSamplerId(plan.getSamplerId());
        task.setSamplerName(plan.getSamplerName());
        task.setSampleType(plan.getSampleType());
        task.setTaskStatus(LabWorkflowConstants.SamplingTaskStatus.PENDING);
        task.setRemark(buildTaskRemark(plan, manualDispatch));
        samplingTaskMapper.insert(task);
        updatePlanStatus(plan, resolvePlanStatusAfterDispatch(plan));
        return true;
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

    private void validatePlan(SamplingPlan plan) {
        if (plan.getStartTime() == null) {
            throw new BusinessException("采样计划开始时间不能为空");
        }
        if (StrUtil.isBlank(plan.getCycleType())) {
            throw new BusinessException("采样计划周期类型不能为空");
        }
        if (!LabWorkflowConstants.CYCLE_TYPES.contains(plan.getCycleType())) {
            throw new BusinessException("采样计划周期类型不合法");
        }
        if (plan.getEndTime() != null && plan.getEndTime().isBefore(plan.getStartTime())) {
            throw new BusinessException("采样计划截止时间不能早于开始时间");
        }
        if (LabWorkflowConstants.isRecurringCycle(plan.getCycleType()) && plan.getEndTime() == null) {
            throw new BusinessException("周期计划必须设置截止时间");
        }
    }

    private LocalDateTime resolveScheduledTime(SamplingPlan plan, LocalDateTime now) {
        if (plan.getStartTime() == null || now == null) {
            return null;
        }
        if (plan.getStartTime().isAfter(now)) {
            return null;
        }
        if (plan.getEndTime() != null && plan.getEndTime().isBefore(now.toLocalDate().atTime(LocalTime.MAX))) {
            if (!plan.getEndTime().isAfter(now)) {
                markPlanCompletedIfExpired(plan, now);
            }
        }
        if (LabWorkflowConstants.isOnceCycle(plan.getCycleType())) {
            if (plan.getEndTime() != null && plan.getEndTime().isBefore(plan.getStartTime())) {
                return null;
            }
            return plan.getStartTime();
        }

        LocalDate scheduleDate = resolveScheduleDate(plan, now.toLocalDate());
        if (scheduleDate == null) {
            return null;
        }
        LocalDateTime scheduledTime = LocalDateTime.of(scheduleDate, plan.getStartTime().toLocalTime());
        if (scheduledTime.isAfter(now)) {
            return null;
        }
        if (scheduledTime.isBefore(plan.getStartTime())) {
            return null;
        }
        if (plan.getEndTime() != null && scheduledTime.isAfter(plan.getEndTime())) {
            markPlanCompletedIfExpired(plan, now);
            return null;
        }
        return scheduledTime;
    }

    private LocalDate resolveScheduleDate(SamplingPlan plan, LocalDate today) {
        if (LabWorkflowConstants.CycleType.DAILY.equals(plan.getCycleType())) {
            return today;
        }
        if (LabWorkflowConstants.CycleType.WEEKLY.equals(plan.getCycleType())) {
            DayOfWeek targetDay = plan.getStartTime().getDayOfWeek();
            return today.getDayOfWeek() == targetDay ? today : null;
        }
        if (LabWorkflowConstants.CycleType.MONTHLY.equals(plan.getCycleType())) {
            int dayOfMonth = plan.getStartTime().getDayOfMonth();
            return today.getDayOfMonth() == dayOfMonth ? today : null;
        }
        return null;
    }

    private String resolvePlanStatusAfterDispatch(SamplingPlan plan) {
        return LabWorkflowConstants.isRecurringCycle(plan.getCycleType())
                ? LabWorkflowConstants.SamplingPlanStatus.ACTIVE
                : LabWorkflowConstants.SamplingPlanStatus.DISPATCHED;
    }

    private String buildTaskRemark(SamplingPlan plan, boolean manualDispatch) {
        if (manualDispatch) {
            return "由采样计划手工派发生成";
        }
        if (LabWorkflowConstants.isRecurringCycle(plan.getCycleType())) {
            return "由周期采样计划自动生成";
        }
        return "由采样计划自动派发生成";
    }

    private void markPlanCompletedIfExpired(SamplingPlan plan, LocalDateTime now) {
        if (plan.getEndTime() != null
                && !plan.getEndTime().isAfter(now)
                && !LabWorkflowConstants.SamplingPlanStatus.COMPLETED.equals(plan.getPlanStatus())) {
            updatePlanStatus(plan, LabWorkflowConstants.SamplingPlanStatus.COMPLETED);
        }
    }

    private void updatePlanStatus(SamplingPlan plan, String planStatus) {
        if (plan == null || StrUtil.equals(plan.getPlanStatus(), planStatus)) {
            return;
        }
        plan.setPlanStatus(planStatus);
        plan.setUpdatedTime(LocalDateTime.now());
        samplingPlanMapper.updateById(plan);
    }
}
