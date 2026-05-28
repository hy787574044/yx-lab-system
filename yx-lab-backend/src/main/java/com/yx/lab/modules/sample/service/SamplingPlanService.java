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
import com.yx.lab.modules.detection.dto.DetectionTypeParameterMethodBindingItem;
import com.yx.lab.modules.detection.entity.DetectionMethod;
import com.yx.lab.modules.detection.entity.DetectionParameter;
import com.yx.lab.modules.detection.entity.DetectionType;
import com.yx.lab.modules.detection.mapper.DetectionMethodMapper;
import com.yx.lab.modules.detection.mapper.DetectionParameterMapper;
import com.yx.lab.modules.detection.mapper.DetectionTypeMapper;
import com.yx.lab.modules.sample.dto.SampleDetectionConfigItem;
import com.yx.lab.modules.sample.dto.SamplingPlanDispatchCommand;
import com.yx.lab.modules.sample.dto.SamplingPlanQuery;
import com.yx.lab.modules.sample.dto.SamplingPlanSaveCommand;
import com.yx.lab.modules.sample.entity.SamplingPlan;
import com.yx.lab.modules.sample.entity.SamplingTask;
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
import java.util.Arrays;
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

    private final DetectionTypeMapper detectionTypeMapper;

    private final DetectionParameterMapper detectionParameterMapper;

    private final DetectionMethodMapper detectionMethodMapper;

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
        Page<SamplingPlan> page = samplingPlanMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<SamplingPlan>()
                        .like(StrUtil.isNotBlank(query.getKeyword()), SamplingPlan::getPlanName, query.getKeyword())
                        .eq(StrUtil.isNotBlank(query.getPlanStatus()), SamplingPlan::getPlanStatus, query.getPlanStatus())
                        .eq(resolveScopedSamplerId(query.getSamplerId()) != null,
                                SamplingPlan::getSamplerId,
                                resolveScopedSamplerId(query.getSamplerId()))
                        .orderByDesc(SamplingPlan::getCreatedTime));
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
        return samplingPlanMapper.selectCount(new LambdaQueryWrapper<SamplingPlan>()
                .eq(StrUtil.isNotBlank(planStatus), SamplingPlan::getPlanStatus, planStatus)
                .eq(resolveScopedSamplerId(null) != null,
                        SamplingPlan::getSamplerId,
                        resolveScopedSamplerId(null)));
    }

    private Long countActiveMissingSamplerPlans() {
        return samplingPlanMapper.selectCount(new LambdaQueryWrapper<SamplingPlan>()
                .eq(SamplingPlan::getPlanStatus, LabWorkflowConstants.SamplingPlanStatus.ACTIVE)
                .and(wrapper -> wrapper
                        .isNull(SamplingPlan::getSamplerId)
                        .or()
                        .isNull(SamplingPlan::getSamplerName)
                        .or()
                        .eq(SamplingPlan::getSamplerName, ""))
                .eq(resolveScopedSamplerId(null) != null,
                        SamplingPlan::getSamplerId,
                        resolveScopedSamplerId(null)));
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

    /**
     * 获取采样计划详情。
     *
     * @param id 计划ID
     * @return 采样计划详情
     */
    public SamplingPlan detail(Long id) {
        return requirePlan(id);
    }

    /**
     * 新增采样计划。
     *
     * @param command 计划保存参数
     */
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
        if (command != null) {
            dispatchPlanTaskWithLock(
                    command.getPlanId(),
                    command.getSamplingTime(),
                    true,
                    command.getSamplerId(),
                    command.getSamplerName());
            return;
        }
        SamplingPlan plan = requirePlan(command.getPlanId());
        if (!LabWorkflowConstants.canDispatchPlan(plan.getPlanStatus())) {
            throw new BusinessException("当前计划状态不允许派发");
        }
        if (command.getSamplerId() != null) {
            plan.setSamplerId(command.getSamplerId());
            plan.setSamplerName(StrUtil.trim(command.getSamplerName()));
            samplingPlanMapper.updateById(plan);
        }
        dispatchPlanTaskWithLock(
                command.getPlanId(),
                command.getSamplingTime(),
                true,
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
                        LabWorkflowConstants.CycleType.MONTHLY)
                .orderByAsc(SamplingPlan::getStartTime))) {
            LocalDateTime scheduledTime = resolveScheduledTime(plan, dispatchTime);
            if (scheduledTime == null) {
                continue;
            }
            if (dispatchPlanTaskWithLock(plan.getId(), scheduledTime, false, null, null)) {
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
                                             Long samplerId,
                                             String samplerName) {
        // 计划级锁控制页面派发与定时派发串行执行，避免同一计划短时间内重复落任务。
        ReentrantLock lock = dispatchLocks.computeIfAbsent(planId, key -> new ReentrantLock());
        lock.lock();
        try {
            SamplingPlan plan = requirePlan(planId);
            if (!LabWorkflowConstants.canDispatchPlan(plan.getPlanStatus())) {
                if (manualDispatch) {
                    throw new BusinessException("褰撳墠璁″垝鐘舵€佷笉鍏佽娲惧彂");
                }
                return false;
            }
            if (samplerId != null) {
                plan.setSamplerId(samplerId);
                plan.setSamplerName(StrUtil.trim(samplerName));
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
        Long existingCount = samplingTaskMapper.selectCount(existingTaskQuery);
        if (existingCount != null && existingCount > 0) {
            if (manualDispatch) {
                throw new BusinessException("当前计划在该执行时间已生成采样任务，不能重复派发");
            }
            return false;
        }
        SamplingTask task = new SamplingTask();
        task.setTaskNo(generateTaskNo());
        task.setSampleNo(sampleNoGeneratorService.nextSampleNo());
        task.setPlanId(plan.getId());
        task.setPointId(plan.getPointId());
        task.setPointName(plan.getPointName());
        task.setSamplingTime(taskTime);
        task.setSamplerId(plan.getSamplerId());
        task.setSamplerName(plan.getSamplerName());
        task.setSampleType(plan.getSampleType());
        task.setSampleRegisterStatus(LabWorkflowConstants.SampleRegisterStatus.UNREGISTERED);
        task.setSampleId(null);
        task.setDetectionItems(plan.getDetectionTypeName());
        task.setDetectionTypeId(plan.getDetectionTypeId());
        task.setDetectionTypeName(plan.getDetectionTypeName());
        task.setDetectionConfigSnapshot(resolvePlanDetectionConfigSnapshot(plan));
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
        DetectionType detectionType = requireEnabledDetectionType(command.getDetectionTypeId(), command.getDetectionTypeName());
        plan.setDetectionTypeId(detectionType.getId());
        plan.setDetectionTypeName(detectionType.getTypeName());
        plan.setDetectionConfigSnapshot(serializeDetectionConfigItems(
                normalizeDetectionConfigItems(command.getDetectionConfigItems(), detectionType)));
        plan.setSamplingBasis(normalizeSamplingBasis(command.getSamplingBasisList()));
        plan.setCycleType(StrUtil.trim(command.getCycleType()));
        plan.setPlanStatus(StrUtil.trim(command.getPlanStatus()));
        plan.setRemark(StrUtil.trim(command.getRemark()));
    }

    private void validatePlan(SamplingPlan plan) {
        if (StrUtil.isBlank(plan.getPointName())) {
            throw new BusinessException("采样点位名称不能为空");
        }
        if (plan.getStartTime() == null) {
            throw new BusinessException("采样计划开始时间不能为空");
        }
        if (StrUtil.isBlank(plan.getCycleType())) {
            throw new BusinessException("采样计划周期类型不能为空");
        }
        if (plan.getDetectionTypeId() == null || StrUtil.isBlank(plan.getDetectionTypeName())) {
            throw new BusinessException("采样计划必须选择检测套餐");
        }
        if (parseDetectionConfigSnapshot(plan.getDetectionConfigSnapshot()).isEmpty()) {
            throw new BusinessException("采样计划必须配置检测套餐参数");
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

    private DetectionType requireEnabledDetectionType(Long detectionTypeId, String submittedTypeName) {
        if (detectionTypeId == null) {
            throw new BusinessException("请选择检测套餐");
        }
        DetectionType detectionType = detectionTypeMapper.selectById(detectionTypeId);
        if (detectionType == null) {
            throw new BusinessException("检测套餐不存在");
        }
        if (!Integer.valueOf(1).equals(detectionType.getEnabled())) {
            throw new BusinessException("当前检测套餐已停用，不能用于采样计划");
        }
        if (StrUtil.isNotBlank(submittedTypeName)
                && !StrUtil.equals(StrUtil.trim(submittedTypeName), detectionType.getTypeName())) {
            throw new BusinessException("检测套餐名称与配置不一致，请刷新后重试");
        }
        return detectionType;
    }

    private String buildDetectionConfigSnapshot(DetectionType detectionType) {
        return serializeDetectionConfigItems(buildDetectionConfigItems(detectionType));
    }

    private String resolvePlanDetectionConfigSnapshot(SamplingPlan plan) {
        if (plan == null) {
            throw new BusinessException("采样计划不存在");
        }
        if (!parseDetectionConfigSnapshot(plan.getDetectionConfigSnapshot()).isEmpty()) {
            return plan.getDetectionConfigSnapshot();
        }
        return buildDetectionConfigSnapshot(requireEnabledDetectionType(plan.getDetectionTypeId(), plan.getDetectionTypeName()));
    }

    private List<SampleDetectionConfigItem> buildDetectionConfigItems(DetectionType detectionType) {
        if (detectionType == null) {
            throw new BusinessException("请选择检测套餐");
        }
        List<Long> parameterIds = parseIdList(detectionType.getParameterIds());
        if (parameterIds.isEmpty()) {
            throw new BusinessException("当前检测套餐未配置检测参数，不能用于采样计划");
        }

        Map<Long, DetectionParameter> parameterMap = detectionParameterMapper.selectList(
                        new LambdaQueryWrapper<DetectionParameter>().in(DetectionParameter::getId, parameterIds))
                .stream()
                .collect(Collectors.toMap(DetectionParameter::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        Map<Long, Long> methodIdByParameterId = parseBindingItems(detectionType.getParameterMethodBindings()).stream()
                .filter(item -> item != null && item.getParameterId() != null && item.getMethodIds() != null && !item.getMethodIds().isEmpty())
                .collect(Collectors.toMap(
                        DetectionTypeParameterMethodBindingItem::getParameterId,
                        item -> item.getMethodIds().get(0),
                        (left, right) -> left,
                        LinkedHashMap::new));

        List<Long> methodIds = methodIdByParameterId.values().stream()
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, DetectionMethod> methodMap = methodIds.isEmpty()
                ? Collections.emptyMap()
                : detectionMethodMapper.selectList(new LambdaQueryWrapper<DetectionMethod>().in(DetectionMethod::getId, methodIds))
                .stream()
                .collect(Collectors.toMap(DetectionMethod::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        return parameterIds.stream()
                .map(parameterId -> buildSnapshotItem(parameterId, parameterMap.get(parameterId), methodMap.get(methodIdByParameterId.get(parameterId))))
                .collect(Collectors.toList());
    }

    private List<SampleDetectionConfigItem> normalizeDetectionConfigItems(List<SampleDetectionConfigItem> submittedItems,
                                                                          DetectionType detectionType) {
        List<SampleDetectionConfigItem> items = submittedItems == null || submittedItems.isEmpty()
                ? buildDetectionConfigItems(detectionType)
                : submittedItems;
        if (items.isEmpty()) {
            throw new BusinessException("请选择检测套餐对应的检测参数与检测方法");
        }

        List<Long> parameterIds = items.stream()
                .map(SampleDetectionConfigItem::getParameterId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (parameterIds.size() != items.size()) {
            throw new BusinessException("检测套餐参数明细存在空项或重复项");
        }

        Map<Long, DetectionParameter> parameterMap = detectionParameterMapper.selectList(
                        new LambdaQueryWrapper<DetectionParameter>().in(DetectionParameter::getId, parameterIds))
                .stream()
                .collect(Collectors.toMap(DetectionParameter::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<Long> methodIds = items.stream()
                .map(SampleDetectionConfigItem::getMethodId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (methodIds.size() != items.size()) {
            throw new BusinessException("请为每个检测参数选择对应的检测方法");
        }
        Map<Long, DetectionMethod> methodMap = detectionMethodMapper.selectList(
                        new LambdaQueryWrapper<DetectionMethod>().in(DetectionMethod::getId, methodIds))
                .stream()
                .collect(Collectors.toMap(DetectionMethod::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        return items.stream()
                .map(item -> buildSnapshotItem(item.getParameterId(), parameterMap.get(item.getParameterId()), methodMap.get(item.getMethodId())))
                .collect(Collectors.toList());
    }

    private String serializeDetectionConfigItems(List<SampleDetectionConfigItem> snapshotItems) {
        try {
            return objectMapper.writeValueAsString(snapshotItems);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("检测套餐参数快照生成失败");
        }
    }

    private List<SampleDetectionConfigItem> parseDetectionConfigSnapshot(String snapshotText) {
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

    private SampleDetectionConfigItem buildSnapshotItem(Long parameterId,
                                                        DetectionParameter parameter,
                                                        DetectionMethod method) {
        if (parameter == null || !Integer.valueOf(1).equals(parameter.getEnabled())) {
            throw new BusinessException("检测套餐包含不存在或已停用的检测参数：" + parameterId);
        }
        if (method == null || !Integer.valueOf(1).equals(method.getEnabled())) {
            throw new BusinessException("检测套餐参数“" + parameter.getParameterName() + "”未配置可用检测方法");
        }
        if (!parameterId.equals(method.getParameterId())) {
            throw new BusinessException("检测套餐参数“" + parameter.getParameterName() + "”绑定的检测方法不匹配");
        }

        SampleDetectionConfigItem item = new SampleDetectionConfigItem();
        item.setParameterId(parameter.getId());
        item.setParameterName(parameter.getParameterName());
        item.setParameterCategory(parameter.getParameterCategory());
        item.setUnit(parameter.getUnit());
        item.setStandardMin(parameter.getStandardMin());
        item.setStandardMax(parameter.getStandardMax());
        item.setReferenceStandard(parameter.getReferenceStandard());
        item.setMethodId(method.getId());
        item.setMethodName(method.getMethodName());
        item.setSampleVolume(method.getSampleVolume());
        item.setMethodBasis(method.getMethodBasis());
        return item;
    }

    private List<Long> parseIdList(String value) {
        if (StrUtil.isBlank(value)) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .map(item -> {
                    try {
                        return Long.valueOf(item);
                    } catch (NumberFormatException ex) {
                        throw new BusinessException("检测套餐参数配置格式错误：" + item);
                    }
                })
                .collect(Collectors.toList());
    }

    private List<DetectionTypeParameterMethodBindingItem> parseBindingItems(String value) {
        if (StrUtil.isBlank(value)) {
            return Collections.emptyList();
        }
        try {
            List<DetectionTypeParameterMethodBindingItem> items = objectMapper.readValue(
                    value,
                    new TypeReference<List<DetectionTypeParameterMethodBindingItem>>() {
                    }
            );
            return items == null ? Collections.emptyList() : items;
        } catch (JsonProcessingException ex) {
            throw new BusinessException("检测套餐参数方法配置格式错误");
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

    private String normalizeSamplingBasis(List<String> samplingBasisList) {
        List<String> items = samplingBasisList == null ? Collections.emptyList() : samplingBasisList.stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        return items.isEmpty() ? null : String.join("、", items);
    }

    private String generateTaskNo() {
        String prefix = "TASK" + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        Long count = samplingTaskMapper.selectCount(new LambdaQueryWrapper<SamplingTask>()
                .likeRight(SamplingTask::getTaskNo, prefix));
        long next = count == null ? 1L : count + 1L;
        return prefix + String.format("%04d", next);
    }
}
