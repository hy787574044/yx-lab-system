package com.yx.lab.modules.sample.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.DataScopeHelper;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.detection.entity.DetectionParameter;
import com.yx.lab.modules.detection.mapper.DetectionParameterMapper;
import com.yx.lab.modules.sample.dto.SampleDetectionConfigItem;
import com.yx.lab.modules.sample.dto.SamplingPlanDispatchCommand;
import com.yx.lab.modules.sample.dto.SamplingPlanQuery;
import com.yx.lab.modules.sample.dto.SamplingPlanSaveCommand;
import com.yx.lab.modules.sample.entity.MonitoringPoint;
import com.yx.lab.modules.sample.entity.SamplingPlan;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.mapper.MonitoringPointMapper;
import com.yx.lab.modules.sample.mapper.SamplingPlanMapper;
import com.yx.lab.modules.sample.mapper.SamplingTaskMapper;
import com.yx.lab.modules.sample.vo.StatusCountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SamplingPlanService {

    private final SamplingPlanMapper samplingPlanMapper;

    private final SamplingTaskMapper samplingTaskMapper;

    private final MonitoringPointMapper monitoringPointMapper;

    private final DetectionParameterMapper detectionParameterMapper;

    private final ObjectMapper objectMapper;

    private final DataScopeHelper dataScopeHelper;

    private final SampleNoGeneratorService sampleNoGeneratorService;

    private final ConcurrentMap<Long, ReentrantLock> dispatchLocks = new ConcurrentHashMap<>();

    /**
     * 分页查询采样计划。
     *
     * @param query 查询条件
     * @return 采样计划分页结果
     */
    public PageResult<SamplingPlan> page(SamplingPlanQuery query) {
        Long scopedSamplerId = resolveScopedSamplerId(query.getSamplerId());
        LambdaQueryWrapper<SamplingPlan> wrapper = new LambdaQueryWrapper<SamplingPlan>()
                .like(StrUtil.isNotBlank(query.getKeyword()), SamplingPlan::getPlanName, query.getKeyword())
                .eq(StrUtil.isNotBlank(query.getPlanStatus()), SamplingPlan::getPlanStatus, query.getPlanStatus())
                .eq(query.getOrgId() != null, SamplingPlan::getOrgId, query.getOrgId());
        applyPlanSamplerScope(wrapper, scopedSamplerId);
        wrapper.orderByDesc(SamplingPlan::getCreatedTime);
        Page<SamplingPlan> page = samplingPlanMapper.selectPage(PageUtils.buildPage(query), wrapper);
        page.getRecords().forEach(this::enrichPlanDetectionConfigSnapshotForView);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    /**
     * 按计划状态统计当前用户可见范围内的采样计划数量。
     *
     * @return 状态数量列表
     */
    public List<StatusCountVO> statusStats() {
        return java.util.Arrays.asList(
                statusCount("ALL", countPlansByStatus(null)),
                statusCount(LabWorkflowConstants.SamplingPlanStatus.ACTIVE,
                        countPlansByStatus(LabWorkflowConstants.SamplingPlanStatus.ACTIVE)),
                statusCount("MISSING_SAMPLER", countActiveMissingSamplerPlans()),
                statusCount(LabWorkflowConstants.SamplingPlanStatus.PAUSED,
                        countPlansByStatus(LabWorkflowConstants.SamplingPlanStatus.PAUSED)),
                statusCount(LabWorkflowConstants.SamplingPlanStatus.DISPATCHED,
                        countPlansByStatus(LabWorkflowConstants.SamplingPlanStatus.DISPATCHED)),
                statusCount(LabWorkflowConstants.SamplingPlanStatus.COMPLETED,
                        countPlansByStatus(LabWorkflowConstants.SamplingPlanStatus.COMPLETED)));
    }

    private StatusCountVO statusCount(String status, Long count) {
        StatusCountVO vo = new StatusCountVO();
        vo.setStatus(status);
        vo.setCount(count == null ? 0L : count);
        return vo;
    }

    private Long countPlansByStatus(String planStatus) {
        LambdaQueryWrapper<SamplingPlan> wrapper = new LambdaQueryWrapper<SamplingPlan>()
                .eq(StrUtil.isNotBlank(planStatus), SamplingPlan::getPlanStatus, planStatus);
        applyPlanSamplerScope(wrapper, resolveScopedSamplerId(null));
        Long count = samplingPlanMapper.selectCount(wrapper);
        return count == null ? 0L : count.longValue();
    }

    private Long countActiveMissingSamplerPlans() {
        LambdaQueryWrapper<SamplingPlan> wrapper = new LambdaQueryWrapper<SamplingPlan>()
                .eq(SamplingPlan::getPlanStatus, LabWorkflowConstants.SamplingPlanStatus.ACTIVE)
                .and(item -> item
                        .isNull(SamplingPlan::getSamplerId)
                        .or()
                        .isNull(SamplingPlan::getSamplerName)
                        .or()
                        .eq(SamplingPlan::getSamplerName, ""));
        applyPlanSamplerScope(wrapper, resolveScopedSamplerId(null));
        Long count = samplingPlanMapper.selectCount(wrapper);
        return count == null ? 0L : count.longValue();
    }

    private Long resolveScopedSamplerId(Long querySamplerId) {
        if (dataScopeHelper.isAdmin()) {
            return querySamplerId;
        }
        if (dataScopeHelper.isRole("STAFF") && dataScopeHelper.currentUserId() != null) {
            return dataScopeHelper.currentUserId();
        }
        return querySamplerId;
    }

    /**
     * 获取采样计划详情。
     *
     * @param id 计划ID
     * @return 采样计划详情
     */
    public SamplingPlan detail(Long id) {
        SamplingPlan plan = requirePlan(id);
        enrichPlanDetectionConfigSnapshotForView(plan);
        return plan;
    }

    /**
     * 新增采样计划。
     *
     * @param command 计划保存参数
     */
    @Transactional(rollbackFor = Exception.class)
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
        LocalDateTime scheduledTime = resolveScheduledTime(plan, LocalDateTime.now());
        if (scheduledTime != null) {
            dispatchPlanTaskWithLock(plan.getId(), scheduledTime, false, null, null, null);
        }
    }

    /**
     * 更新采样计划。
     *
     * @param id 计划ID
     * @param command 计划保存参数
     */
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

    /**
     * 删除采样计划。
     *
     * @param id 计划ID
     */
    public void delete(Long id) {
        SamplingPlan existing = requirePlan(id);
        if (LabWorkflowConstants.isLockedPlan(existing.getPlanStatus())) {
            throw new BusinessException("当前计划已执行，不允许删除");
        }
        samplingPlanMapper.deleteById(id);
    }

    /**
     * 暂停采样计划。
     *
     * @param id 计划ID
     */
    public void pause(Long id) {
        SamplingPlan plan = requirePlan(id);
        if (!LabWorkflowConstants.canPausePlan(plan.getPlanStatus())) {
            throw new BusinessException("当前计划状态不允许暂停");
        }
        plan.setPlanStatus(LabWorkflowConstants.SamplingPlanStatus.PAUSED);
        samplingPlanMapper.updateById(plan);
    }

    /**
     * 恢复采样计划。
     *
     * @param id 计划ID
     */
    public void resume(Long id) {
        SamplingPlan plan = requirePlan(id);
        if (!LabWorkflowConstants.canResumePlan(plan.getPlanStatus())) {
            throw new BusinessException("当前计划不处于暂停状态");
        }
        plan.setPlanStatus(LabWorkflowConstants.SamplingPlanStatus.ACTIVE);
        samplingPlanMapper.updateById(plan);
    }

    /**
     * 手工派发采样计划生成任务。
     *
     * @param command 派发参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void dispatch(SamplingPlanDispatchCommand command) {
        // 页面手工派发统一走带锁入口，避免和定时派发并发重复生成任务。
        if (command == null) {
            throw new BusinessException("派发参数不能为空");
        }
        dispatchPlanTaskWithLock(
                command.getPlanId(),
                command.getSamplingTime(),
                true,
                command.getSamplerIds(),
                command.getSamplerId(),
                command.getSamplerName());
    }

    /**
     * 自动扫描到期计划并批量派发任务。
     *
     * @param now 当前时间，允许外部传入测试时间
     * @return 本次成功派发的任务数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int autoDispatchDuePlans(LocalDateTime now) {
        LocalDateTime dispatchTime = now == null ? LocalDateTime.now() : now;
        int dispatchedCount = 0;
        // 定时任务只扫描启用中的周期计划，并按开始时间顺序尝试派发。
        for (SamplingPlan plan : samplingPlanMapper.selectList(new LambdaQueryWrapper<SamplingPlan>()
                .eq(SamplingPlan::getPlanStatus, LabWorkflowConstants.SamplingPlanStatus.ACTIVE)
                .in(SamplingPlan::getCycleType,
                        LabWorkflowConstants.CycleType.ONCE,
                        LabWorkflowConstants.CycleType.DAILY,
                        LabWorkflowConstants.CycleType.WEEKLY,
                        LabWorkflowConstants.CycleType.HALF_MONTHLY,
                        LabWorkflowConstants.CycleType.MONTHLY)
                .orderByAsc(SamplingPlan::getStartTime))) {
            LocalDateTime scheduledTime = resolveScheduledTime(plan, dispatchTime);
            if (scheduledTime == null) {
                continue;
            }
            if (dispatchPlanTaskWithLock(plan.getId(), scheduledTime, false, null, null, null)) {
                dispatchedCount++;
            }
        }
        return dispatchedCount;
    }

    /**
     * 在任务完成后刷新计划状态。
     *
     * @param planId 计划ID
     */
    public void refreshPlanStatusAfterTaskCompletion(Long planId) {
        refreshPlanStatusAfterTaskChange(planId);
    }

    /**
     * 在任务状态变化后重算计划状态。
     *
     * @param planId 计划ID
     */
    public void refreshPlanStatusAfterTaskChange(Long planId) {
        if (planId == null) {
            return;
        }
        SamplingPlan plan = samplingPlanMapper.selectById(planId);
        if (plan == null || LabWorkflowConstants.SamplingPlanStatus.PAUSED.equals(plan.getPlanStatus())) {
            return;
        }
        List<SamplingTask> planTasks = samplingTaskMapper.selectList(new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getPlanId, planId)
                .orderByDesc(SamplingTask::getCreatedTime));
        LocalDateTime now = LocalDateTime.now();
        boolean hasTodoTask = planTasks.stream().anyMatch(task ->
                LabWorkflowConstants.TODO_TASK_STATUSES.contains(task.getTaskStatus()));
        boolean hasCompletedTask = planTasks.stream().anyMatch(task ->
                LabWorkflowConstants.SamplingTaskStatus.COMPLETED.equals(task.getTaskStatus()));
        boolean expired = plan.getEndTime() != null && !plan.getEndTime().isAfter(now);

        if (LabWorkflowConstants.isOnceCycle(plan.getCycleType())) {
            if (hasCompletedTask || expired) {
                updatePlanStatus(plan, LabWorkflowConstants.SamplingPlanStatus.COMPLETED);
                return;
            }
            if (hasTodoTask) {
                updatePlanStatus(plan, LabWorkflowConstants.SamplingPlanStatus.DISPATCHED);
                return;
            }
            updatePlanStatus(plan, LabWorkflowConstants.SamplingPlanStatus.ACTIVE);
            return;
        }

        if (expired && !hasTodoTask) {
            updatePlanStatus(plan, LabWorkflowConstants.SamplingPlanStatus.COMPLETED);
            return;
        }
        updatePlanStatus(plan, LabWorkflowConstants.SamplingPlanStatus.ACTIVE);
    }

    /**
     * 按ID获取采样计划，不存在时抛出业务异常。
     *
     * @param id 计划ID
     * @return 采样计划
     */
    public SamplingPlan requirePlan(Long id) {
        SamplingPlan plan = samplingPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("采样计划不存在");
        }
        return plan;
    }

    private boolean dispatchPlanTaskWithLock(Long planId,
                                             LocalDateTime samplingTime,
                                             boolean manualDispatch,
                                             List<Long> samplerIds,
                                             Long samplerId,
                                             String samplerName) {
        // 计划级锁控制页面派发与定时派发串行执行，避免同一计划短时间内重复落任务。
        ReentrantLock lock = dispatchLocks.computeIfAbsent(planId, key -> new ReentrantLock());
        lock.lock();
        try {
            SamplingPlan plan = requirePlan(planId);
            if (!LabWorkflowConstants.canDispatchPlan(plan.getPlanStatus())) {
                if (manualDispatch) {
                    throw new BusinessException("当前计划状态不允许派发");
                }
                return false;
            }
            List<Long> targetSamplerIds = normalizeSamplerIds(samplerIds, samplerId);
            if (!targetSamplerIds.isEmpty()) {
                applySamplerSnapshot(plan, targetSamplerIds, samplerName);
                samplingPlanMapper.updateById(plan);
            }
            return dispatchPlanTask(plan, samplingTime, manualDispatch);
        } finally {
            lock.unlock();
        }
    }

    private boolean dispatchPlanTask(SamplingPlan plan, LocalDateTime samplingTime, boolean manualDispatch) {
        if (plan.getSamplerId() == null || StrUtil.isBlank(plan.getSamplerName())) {
            if (manualDispatch) {
                throw new BusinessException("派发采样任务前必须指定采样员");
            }
            log.warn("auto dispatch skipped plan without sampler, planId={}, planName={}", plan.getId(), plan.getPlanName());
            return false;
        }
        // 任务执行时间优先取本次派发指定时间，否则回落到计划开始时间。
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
        // 同一计划在同一采样时间只允许生成一条有效任务，避免重复执行。
        LambdaQueryWrapper<SamplingTask> existingTaskQuery = new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getPlanId, plan.getId())
                .eq(SamplingTask::getSamplingTime, taskTime);
        if (manualDispatch) {
            existingTaskQuery.ne(SamplingTask::getTaskStatus, LabWorkflowConstants.SamplingTaskStatus.ABANDONED);
        }
        List<SamplingTask> existingTasks = samplingTaskMapper.selectList(existingTaskQuery);
        if (!existingTasks.isEmpty()) {
            if (manualDispatch) {
                throw new BusinessException("当前计划在该执行时间已生成采样任务，不能重复派发");
            }
            fillMissingAutoDispatchTaskFields(plan, existingTasks);
            return false;
        }
        SamplingTask task = new SamplingTask();
        task.setTaskNo(generateTaskNo());
        task.setSampleNo(sampleNoGeneratorService.ensureSampleNo(null));
        task.setPlanId(plan.getId());
        task.setOrgId(plan.getOrgId());
        task.setPointId(plan.getPointId());
        task.setPointName(plan.getPointName());
        task.setAddress(plan.getAddress());
        task.setLatitude(plan.getLatitude());
        task.setLongitude(plan.getLongitude());
        task.setSamplingTime(taskTime);
        task.setSamplerId(plan.getSamplerId());
        task.setSamplerIds(plan.getSamplerIds());
        task.setSamplerName(plan.getSamplerName());
        task.setSampleType(plan.getSampleType());
        task.setSampleRegisterStatus(LabWorkflowConstants.SampleRegisterStatus.UNREGISTERED);
        task.setSampleId(null);
        task.setTaskStatus(LabWorkflowConstants.SamplingTaskStatus.PENDING);
        task.setRemark(buildTaskRemark(plan, manualDispatch));
        samplingTaskMapper.insert(task);
        updatePlanStatus(plan, resolvePlanStatusAfterDispatch(plan));
        return true;
    }

    private void fillMissingAutoDispatchTaskFields(SamplingPlan plan, List<SamplingTask> existingTasks) {
        for (SamplingTask existingTask : existingTasks) {
            boolean changed = false;
            if (StrUtil.isBlank(existingTask.getSampleNo())) {
                existingTask.setSampleNo(sampleNoGeneratorService.ensureSampleNo(existingTask.getSampleNo()));
                changed = true;
            }
            if (existingTask.getOrgId() == null && plan.getOrgId() != null) {
                existingTask.setOrgId(plan.getOrgId());
                changed = true;
            }
            if (existingTask.getPointId() == null && plan.getPointId() != null) {
                existingTask.setPointId(plan.getPointId());
                changed = true;
            }
            if (StrUtil.isBlank(existingTask.getPointName()) && StrUtil.isNotBlank(plan.getPointName())) {
                existingTask.setPointName(plan.getPointName());
                changed = true;
            }
            if (StrUtil.isBlank(existingTask.getAddress()) && StrUtil.isNotBlank(plan.getAddress())) {
                existingTask.setAddress(plan.getAddress());
                changed = true;
            }
            if (StrUtil.isBlank(existingTask.getLatitude()) && StrUtil.isNotBlank(plan.getLatitude())) {
                existingTask.setLatitude(plan.getLatitude());
                changed = true;
            }
            if (StrUtil.isBlank(existingTask.getLongitude()) && StrUtil.isNotBlank(plan.getLongitude())) {
                existingTask.setLongitude(plan.getLongitude());
                changed = true;
            }
            if (existingTask.getSamplerId() == null && plan.getSamplerId() != null) {
                existingTask.setSamplerId(plan.getSamplerId());
                changed = true;
            }
            if (StrUtil.isBlank(existingTask.getSamplerIds()) && StrUtil.isNotBlank(plan.getSamplerIds())) {
                existingTask.setSamplerIds(plan.getSamplerIds());
                changed = true;
            }
            if (StrUtil.isBlank(existingTask.getSamplerName()) && StrUtil.isNotBlank(plan.getSamplerName())) {
                existingTask.setSamplerName(plan.getSamplerName());
                changed = true;
            }
            if (StrUtil.isBlank(existingTask.getSampleType()) && StrUtil.isNotBlank(plan.getSampleType())) {
                existingTask.setSampleType(plan.getSampleType());
                changed = true;
            }
            if (StrUtil.isBlank(existingTask.getSampleRegisterStatus())) {
                existingTask.setSampleRegisterStatus(LabWorkflowConstants.SampleRegisterStatus.UNREGISTERED);
                changed = true;
            }
            if (changed) {
                samplingTaskMapper.updateById(existingTask);
            }
        }
    }

    private void applyPlanCommand(SamplingPlan plan, SamplingPlanSaveCommand command) {
        plan.setPlanName(StrUtil.trim(command.getPlanName()));
        plan.setOrgId(command.getOrgId());
        plan.setPointId(command.getPointId());
        plan.setPointName(StrUtil.trim(command.getPointName()));
        plan.setAddress(StrUtil.trim(command.getAddress()));
        plan.setLatitude(StrUtil.trim(command.getLatitude()));
        plan.setLongitude(StrUtil.trim(command.getLongitude()));
        applyMonitoringPointSnapshot(plan);
        plan.setStartTime(command.getStartTime());
        plan.setEndTime(command.getEndTime());
        applySamplerSnapshot(plan, normalizeSamplerIds(command.getSamplerIds(), command.getSamplerId()), command.getSamplerName());
        plan.setSamplingType(StrUtil.trim(command.getSamplingType()));
        plan.setSampleType(StrUtil.trim(command.getSampleType()));
        plan.setDetectionTypeId(null);
        plan.setDetectionTypeName(null);
        plan.setDetectionConfigSnapshot(null);
        plan.setCycleType(normalizePlanCycleType(command.getCycleType()));
        plan.setPlanStatus(StrUtil.trim(command.getPlanStatus()));
        plan.setRemark(StrUtil.trim(command.getRemark()));
    }

    private String normalizePlanCycleType(String cycleType) {
        String value = StrUtil.trim(cycleType);
        if (StrUtil.isBlank(value)) {
            return value;
        }
        if (LabWorkflowConstants.CYCLE_TYPES.contains(value)) {
            return value;
        }
        if ("一次".equals(value)) {
            return LabWorkflowConstants.CycleType.ONCE;
        }
        if ("每日".equals(value) || "每天".equals(value)) {
            return LabWorkflowConstants.CycleType.DAILY;
        }
        if ("每周".equals(value) || "周".equals(value)) {
            return LabWorkflowConstants.CycleType.WEEKLY;
        }
        if ("半月".equals(value) || "每半月".equals(value)) {
            return LabWorkflowConstants.CycleType.HALF_MONTHLY;
        }
        if ("每月".equals(value) || "月".equals(value)) {
            return LabWorkflowConstants.CycleType.MONTHLY;
        }
        return value;
    }

    private void validatePlan(SamplingPlan plan) {
        if (plan.getOrgId() == null) {
            throw new BusinessException("所属机构不能为空");
        }
        if (StrUtil.isBlank(plan.getPointName())) {
            throw new BusinessException("采样点位名称不能为空");
        }
        if (StrUtil.isBlank(plan.getLatitude()) || StrUtil.isBlank(plan.getLongitude())) {
            throw new BusinessException("采样点位必须选择地图坐标");
        }
        validateCoordinate(plan.getLatitude(), "纬度", -90D, 90D);
        validateCoordinate(plan.getLongitude(), "经度", -180D, 180D);
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

    private void applyMonitoringPointSnapshot(SamplingPlan plan) {
        if (plan.getPointId() == null) {
            return;
        }
        MonitoringPoint point = monitoringPointMapper.selectById(plan.getPointId());
        if (point == null) {
            throw new BusinessException("监测点位不存在");
        }
        if (point.getOrgId() != null) {
            if (plan.getOrgId() != null && !point.getOrgId().equals(plan.getOrgId())) {
                throw new BusinessException("监测点位与所属机构不一致");
            }
            plan.setOrgId(point.getOrgId());
        }
        plan.setPointName(StrUtil.trim(point.getPointName()));
        plan.setLatitude(StrUtil.trim(point.getLatitude()));
        plan.setLongitude(StrUtil.trim(point.getLongitude()));
        if (StrUtil.isNotBlank(point.getPointType())) {
            plan.setSampleType(StrUtil.trim(point.getPointType()));
        }
        if (StrUtil.isBlank(plan.getAddress())) {
            plan.setAddress(StrUtil.blankToDefault(StrUtil.trim(point.getAddress()), StrUtil.trim(point.getPointName())));
        }
    }

    private void validateCoordinate(String value, String label, double min, double max) {
        String text = StrUtil.trim(value);
        try {
            double coordinate = Double.parseDouble(text);
            if (coordinate < min || coordinate > max) {
                throw new BusinessException("采样点位" + label + "超出有效范围");
            }
        } catch (NumberFormatException ex) {
            throw new BusinessException("采样点位" + label + "格式不正确");
        }
    }
    public String serializeDetectionConfigItems(List<SampleDetectionConfigItem> snapshotItems) {
        try {
            return objectMapper.writeValueAsString(snapshotItems);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("检测套餐参数快照生成失败");
        }
    }

    public List<SampleDetectionConfigItem> parseDetectionConfigSnapshot(String snapshotText) {
        if (StrUtil.isBlank(snapshotText)) {
            return Collections.emptyList();
        }
        try {
            List<SampleDetectionConfigItem> items = objectMapper.readValue(
                    snapshotText,
                    new TypeReference<List<SampleDetectionConfigItem>>() {
                    }
            );
            if (items == null) {
                return Collections.emptyList();
            }
            return items.stream()
                    .filter(item -> item != null && item.getParameterId() != null && item.getMethodId() != null)
                    .collect(Collectors.toList());
        } catch (JsonProcessingException ex) {
            throw new BusinessException("采样计划检测套餐参数快照格式错误");
        }
    }
    public String enrichDetectionConfigSnapshotForView(String snapshotText) {
        if (StrUtil.isBlank(snapshotText)) {
            return snapshotText;
        }
        try {
            List<SampleDetectionConfigItem> items = parseDetectionConfigSnapshot(snapshotText);
            if (items.isEmpty()) {
                return snapshotText;
            }
            enrichDetectionConfigItems(items);
            return serializeDetectionConfigItems(items);
        } catch (BusinessException ex) {
            return snapshotText;
        }
    }

    private void enrichPlanDetectionConfigSnapshotForView(SamplingPlan plan) {
        if (plan == null) {
            return;
        }
        plan.setDetectionConfigSnapshot(enrichDetectionConfigSnapshotForView(plan.getDetectionConfigSnapshot()));
    }

    private void enrichDetectionConfigItems(List<SampleDetectionConfigItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Long> parameterIds = items.stream()
                .map(SampleDetectionConfigItem::getParameterId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, DetectionParameter> parameterMap = parameterIds.isEmpty()
                ? Collections.emptyMap()
                : detectionParameterMapper.selectList(new LambdaQueryWrapper<DetectionParameter>()
                        .in(DetectionParameter::getId, parameterIds))
                .stream()
                .collect(Collectors.toMap(DetectionParameter::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        for (SampleDetectionConfigItem item : items) {
            if (item == null) {
                continue;
            }
            DetectionParameter parameter = parameterMap.get(item.getParameterId());
            if (StrUtil.isBlank(item.getParameterName()) && parameter != null) {
                item.setParameterName(parameter.getParameterName());
            }
            item.setResultValue(null);
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
        if (LabWorkflowConstants.CycleType.HALF_MONTHLY.equals(plan.getCycleType())) {
            return resolveHalfMonthlyScheduleDate(plan, today);
        }
        if (LabWorkflowConstants.CycleType.MONTHLY.equals(plan.getCycleType())) {
            int dayOfMonth = plan.getStartTime().getDayOfMonth();
            return today.getDayOfMonth() == dayOfMonth ? today : null;
        }
        return null;
    }

    private LocalDate resolveHalfMonthlyScheduleDate(SamplingPlan plan, LocalDate today) {
        int startDay = plan.getStartTime().getDayOfMonth();
        int firstDay = startDay <= 15 ? startDay : startDay - 15;
        int secondDay = startDay <= 15 ? startDay + 15 : startDay;
        int lastDay = today.lengthOfMonth();
        int normalizedFirstDay = Math.min(firstDay, lastDay);
        int normalizedSecondDay = Math.min(secondDay, lastDay);
        int todayDay = today.getDayOfMonth();
        return todayDay == normalizedFirstDay || todayDay == normalizedSecondDay ? today : null;
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

    private void applyPlanSamplerScope(LambdaQueryWrapper<SamplingPlan> wrapper, Long samplerId) {
        if (wrapper == null || samplerId == null) {
            return;
        }
        wrapper.and(item -> item
                .eq(SamplingPlan::getSamplerId, samplerId)
                .or()
                .like(SamplingPlan::getSamplerIds, wrapSamplerId(samplerId)));
    }

    private List<Long> normalizeSamplerIds(List<Long> samplerIds, Long fallbackSamplerId) {
        List<Long> ids = samplerIds == null ? Collections.emptyList() : samplerIds;
        List<Long> normalized = ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (normalized.isEmpty() && fallbackSamplerId != null && fallbackSamplerId > 0) {
            normalized = Collections.singletonList(fallbackSamplerId);
        }
        return normalized;
    }

    private void applySamplerSnapshot(SamplingPlan plan, List<Long> samplerIds, String samplerName) {
        List<Long> ids = normalizeSamplerIds(samplerIds, null);
        plan.setSamplerId(ids.isEmpty() ? null : ids.get(0));
        plan.setSamplerIds(ids.isEmpty() ? null : "," + ids.stream().map(String::valueOf).collect(Collectors.joining(",")) + ",");
        plan.setSamplerName(StrUtil.trim(samplerName));
    }

    private String wrapSamplerId(Long samplerId) {
        return samplerId == null ? null : "," + samplerId + ",";
    }

    private String generateTaskNo() {
        String prefix = "TASK" + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        Long count = samplingTaskMapper.selectCount(new LambdaQueryWrapper<SamplingTask>()
                .likeRight(SamplingTask::getTaskNo, prefix));
        long next = count == null ? 1L : count.longValue() + 1L;
        return prefix + String.format("%04d", next);
    }
}
