package com.yx.lab.modules.detection.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.modules.detection.dto.DetectionAssignCommand;
import com.yx.lab.modules.detection.dto.DetectionItemAssignCommand;
import com.yx.lab.modules.detection.entity.DetectionAssignmentMemory;
import com.yx.lab.modules.detection.entity.DetectionItem;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.entity.DetectionParameter;
import com.yx.lab.modules.detection.mapper.DetectionAssignmentMemoryMapper;
import com.yx.lab.modules.detection.mapper.DetectionItemMapper;
import com.yx.lab.modules.detection.mapper.DetectionParameterMapper;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.sample.dto.SampleDetectionConfigItem;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.entity.MonitoringPoint;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.sample.mapper.MonitoringPointMapper;
import com.yx.lab.modules.system.entity.LabUser;
import com.yx.lab.modules.system.mapper.LabUserMapper;
import com.yx.lab.modules.system.service.BusinessParticipantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 检测待办流程服务。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DetectionPendingFlowService {

    /**
     * 按样品维度保存并发锁，避免页面派发与自动补偿同时生成重复主流程。
     */
    private final ConcurrentMap<Long, Object> sampleLocks = new ConcurrentHashMap<>();

    private final DetectionRecordMapper detectionRecordMapper;

    private final DetectionItemMapper detectionItemMapper;

    private final DetectionAssignmentMemoryMapper detectionAssignmentMemoryMapper;

    private final DetectionParameterMapper detectionParameterMapper;

    private final LabSampleMapper labSampleMapper;

    private final MonitoringPointMapper monitoringPointMapper;

    private final LabUserMapper labUserMapper;

    private final BusinessParticipantService businessParticipantService;

    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    /**
     * 扫描所有处于待检范围内的样品，并补齐待分配检测主流程。
     */
    public void syncPendingFlowsForOpenSamples() {
        List<LabSample> samples = labSampleMapper.selectList(new LambdaQueryWrapper<LabSample>()
                .in(LabSample::getSampleStatus, LabWorkflowConstants.DETECTABLE_SAMPLE_STATUSES)
                .orderByAsc(LabSample::getCreatedTime));
        if (samples.isEmpty()) {
            return;
        }
        List<Long> sampleIds = samples.stream()
                .map(LabSample::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        Map<Long, List<DetectionRecord>> activeRecordGroup;
        if (sampleIds.isEmpty()) {
            activeRecordGroup = Collections.emptyMap();
        } else {
            List<DetectionRecord> activeRecords = detectionRecordMapper.selectList(new LambdaQueryWrapper<DetectionRecord>()
                    .in(DetectionRecord::getSampleId, sampleIds)
                    .in(DetectionRecord::getDetectionStatus, LabWorkflowConstants.ACTIVE_DETECTION_RECORD_STATUSES));
            activeRecordGroup = activeRecords.stream()
                    .collect(Collectors.groupingBy(DetectionRecord::getSampleId));
        }
        for (LabSample sample : samples) {
            List<DetectionRecord> activeRecords = activeRecordGroup.getOrDefault(sample.getId(), Collections.<DetectionRecord>emptyList());
            if (activeRecords.isEmpty()) {
                createPendingFlowIfMissing(sample);
            } else if (activeRecords.size() > 1) {
                DetectionRecord keeper = collapseDuplicateActiveRecords(sample.getId());
                prefillDetectorIfMissing(keeper, sample);
                promoteEnteredRecordIfReady(keeper, sample);
            } else {
                DetectionRecord record = activeRecords.get(0);
                prefillDetectorIfMissing(record, sample);
                promoteEnteredRecordIfReady(record, sample);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 为指定样品创建待分配检测主流程；若已存在有效流程则直接复用。
     *
     * @param sample 样品信息
     * @return 待检主流程记录
     */
    public DetectionRecord createPendingFlowIfMissing(LabSample sample) {
        if (sample == null || sample.getId() == null) {
            return null;
        }
        Object lock = sampleLocks.computeIfAbsent(sample.getId(), key -> new Object());
        synchronized (lock) {
            // 同一样品始终只保留一条活跃检测主流程，避免重复登录或重复补偿造成脏数据。
            DetectionRecord existing = collapseDuplicateActiveRecords(sample.getId());
            if (existing != null) {
                prefillDetectorIfMissing(existing, sample);
                promoteEnteredRecordIfReady(existing, sample);
                return existing;
            }

            // 样品登录时冻结的套餐参数快照，是后续生成参数子流程的唯一数据来源。
            List<SampleDetectionConfigItem> configItems = parseSampleConfigItems(sample.getDetectionConfigSnapshot());
            if (configItems.isEmpty()) {
                return null;
            }
            Long orgId = resolveSampleOrgId(sample);
            Map<Long, DetectionAssignmentMemory> memoryMap = loadAssignmentMemoryMap(orgId, configItems);
            Map<Long, LabUser> memoryDetectorMap = loadMemoryDetectorMap(memoryMap, orgId);
            LabUser fallbackDetector = businessParticipantService.findDefaultDetectionAssignee(orgId);
            DetectionRecord record = new DetectionRecord();
            record.setSampleId(sample.getId());
            record.setSampleNo(sample.getSampleNo());
            record.setOrgId(orgId);
            record.setDetectionTypeId(sample.getDetectionTypeId());
            record.setDetectionTypeName(resolveDetectionTypeName(sample));
            record.setDetectionTime(sample.getSamplingTime() != null ? sample.getSamplingTime() : LocalDateTime.now());
            record.setDetectorId(null);
            record.setDetectorName(null);
            record.setDetectionResult(null);
            record.setAbnormalRemark("待确认分配检测人员");
            record.setDetectionStatus(LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN);
            detectionRecordMapper.insert(record);

            // 每一个套餐参数都展开成独立子流程，后续可单独分配检测员、录入结果和审查。
            // 批量查询参数配置，用于补全快照中缺失的 optionValues
            Map<Long, DetectionParameter> parameterMap = loadParameterMap(configItems);
            for (SampleDetectionConfigItem configItem : configItems) {
                LabUser initialDetector = resolveInitialDetector(
                        orgId,
                        configItem.getParameterId(),
                        memoryMap,
                        memoryDetectorMap,
                        fallbackDetector);
                DetectionItem item = new DetectionItem();
                item.setRecordId(record.getId());
                item.setOrgId(orgId);
                item.setParameterId(configItem.getParameterId());
                item.setParameterName(configItem.getParameterName());
                item.setStandardMin(configItem.getStandardMin());
                item.setStandardMax(configItem.getStandardMax());
                item.setUnit(configItem.getUnit());
                item.setReferenceStandard(configItem.getReferenceStandard());
                item.setMethodId(configItem.getMethodId());
                item.setMethodName(configItem.getMethodName());
                // 快照中可能没有 optionValues，从参数配置实时补全
                String optionValues = configItem.getOptionValues();
                if (StrUtil.isBlank(optionValues) && configItem.getParameterId() != null) {
                    DetectionParameter param = parameterMap.get(configItem.getParameterId());
                    if (param != null) {
                        optionValues = param.getOptionValues();
                    }
                }
                item.setOptionValues(optionValues);
                item.setDetectorId(initialDetector == null ? null : initialDetector.getId());
                item.setDetectorName(resolveDetectorName(initialDetector));
                item.setItemStatus(LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN);
                item.setExceedFlag(0);
                detectionItemMapper.insert(item);
            }
            return record;
        }
    }

    /**
     * 批量加载参数配置，用于补全快照中缺失的 optionValues。
     */
    private Map<Long, DetectionParameter> loadParameterMap(List<SampleDetectionConfigItem> configItems) {
        Set<Long> parameterIds = configItems.stream()
                .map(SampleDetectionConfigItem::getParameterId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (parameterIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DetectionParameter> parameters = detectionParameterMapper.selectList(
                new LambdaQueryWrapper<DetectionParameter>().in(DetectionParameter::getId, parameterIds));
        return parameters.stream().collect(Collectors.toMap(DetectionParameter::getId, p -> p));
    }

    private Long resolveSampleOrgId(LabSample sample) {
        if (sample == null) {
            return null;
        }
        if (sample.getOrgId() != null) {
            return sample.getOrgId();
        }
        if (sample.getPointId() != null) {
            MonitoringPoint point = monitoringPointMapper.selectById(sample.getPointId());
            if (point != null && point.getOrgId() != null) {
                return point.getOrgId();
            }
        }
        if (sample.getSamplerId() != null) {
            LabUser sampler = labUserMapper.selectById(sample.getSamplerId());
            if (sampler != null) {
                return sampler.getOrgId();
            }
        }
        return null;
    }

    private Map<Long, DetectionAssignmentMemory> loadAssignmentMemoryMap(Long orgId, List<SampleDetectionConfigItem> configItems) {
        if (orgId == null || configItems == null || configItems.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> parameterIds = configItems.stream()
                .map(SampleDetectionConfigItem::getParameterId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (parameterIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return detectionAssignmentMemoryMapper.selectList(new LambdaQueryWrapper<DetectionAssignmentMemory>()
                            .eq(DetectionAssignmentMemory::getOrgId, orgId)
                            .in(DetectionAssignmentMemory::getParameterId, parameterIds))
                    .stream()
                    .collect(Collectors.toMap(DetectionAssignmentMemory::getParameterId, item -> item, (left, right) -> left));
        } catch (Exception ex) {
            if (isMissingAssignmentMemoryTable(ex)) {
                log.warn("检测分配记忆表不存在，已跳过记忆分配读取: {}", ex.getMessage());
                return Collections.emptyMap();
            }
            throw ex;
        }
    }

    private Map<Long, LabUser> loadMemoryDetectorMap(Map<Long, DetectionAssignmentMemory> memoryMap, Long orgId) {
        if (memoryMap == null || memoryMap.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> detectorIds = memoryMap.values().stream()
                .map(DetectionAssignmentMemory::getDetectorId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (detectorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return labUserMapper.selectList(new LambdaQueryWrapper<LabUser>()
                        .in(LabUser::getId, detectorIds)
                        .eq(LabUser::getStatus, 1))
                .stream()
                .filter(user -> businessParticipantService.isDetectionAssignee(user, orgId))
                .collect(Collectors.toMap(LabUser::getId, user -> user, (left, right) -> left));
    }

    private LabUser resolveInitialDetector(Long orgId,
                                           Long parameterId,
                                           Map<Long, DetectionAssignmentMemory> memoryMap,
                                           Map<Long, LabUser> memoryDetectorMap,
                                           LabUser fallbackDetector) {
        if (businessParticipantService.isYanzhenWaterPlant(orgId)
                && fallbackDetector != null
                && StrUtil.equalsIgnoreCase(BusinessParticipantService.DIRECTOR_ROLE_CODE, fallbackDetector.getRoleCode())
                && isUsableDetectionAssignee(fallbackDetector, orgId)) {
            return fallbackDetector;
        }
        DetectionAssignmentMemory memory = memoryMap == null ? null : memoryMap.get(parameterId);
        if (memory != null && memory.getDetectorId() != null) {
            LabUser detector = memoryDetectorMap == null ? null : memoryDetectorMap.get(memory.getDetectorId());
            if (isUsableDetectionAssignee(detector, orgId)) {
                return detector;
            }
        }
        return isUsableDetectionAssignee(fallbackDetector, orgId) ? fallbackDetector : null;
    }

    private boolean isUsableDetectionAssignee(LabUser user, Long orgId) {
        return businessParticipantService.isDetectionAssignee(user, orgId);
    }

    /**
     * 查询样品对应的有效检测主流程。
     *
     * @param sampleId 样品ID
     * @return 有效检测主流程，不存在时返回空
     */
    public DetectionRecord findActiveRecordBySampleId(Long sampleId) {
        if (sampleId == null) {
            return null;
        }
        return collapseDuplicateActiveRecords(sampleId);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 合并同一样品的重复有效主流程，只保留一条继续流转。
     *
     * @param sampleId 样品ID
     * @return 保留下来的主流程记录
     */
    public DetectionRecord collapseDuplicateActiveRecords(Long sampleId) {
        if (sampleId == null) {
            return null;
        }
        List<DetectionRecord> records = detectionRecordMapper.selectList(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getSampleId, sampleId)
                .in(DetectionRecord::getDetectionStatus, LabWorkflowConstants.ACTIVE_DETECTION_RECORD_STATUSES)
                .orderByDesc(DetectionRecord::getCreatedTime)
                .orderByDesc(DetectionRecord::getId));
        if (records.isEmpty()) {
            return null;
        }
        if (records.size() == 1) {
            return records.get(0);
        }

        List<Long> recordIds = records.stream().map(DetectionRecord::getId).collect(Collectors.toList());
        Map<Long, List<DetectionItem>> itemGroup = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                        .in(DetectionItem::getRecordId, recordIds))
                .stream()
                .collect(Collectors.groupingBy(DetectionItem::getRecordId));

        DetectionRecord keeper = records.stream()
                .max(Comparator
                        .comparingInt((DetectionRecord record) -> countCompletedItems(itemGroup.get(record.getId())))
                        .thenComparingInt(record -> countAssignedItems(itemGroup.get(record.getId())))
                        .thenComparingInt(record -> itemGroup.getOrDefault(record.getId(), Collections.emptyList()).size())
                        .thenComparing(DetectionRecord::getUpdatedTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(DetectionRecord::getCreatedTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(DetectionRecord::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(records.get(0));

        for (DetectionRecord record : records) {
            if (record.getId().equals(keeper.getId())) {
                continue;
            }
            softDeleteRecord(record, itemGroup.getOrDefault(record.getId(), Collections.emptyList()));
        }
        return keeper;
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 按检测参数对子流程分配检测员。
     *
     * @param recordId 检测主流程ID
     * @param command 分配参数
     */
    public void assignDetectors(Long recordId, DetectionAssignCommand command) {
        DetectionRecord record = detectionRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("检测主流程不存在");
        }
        if (!LabWorkflowConstants.canAssignDetection(record.getDetectionStatus())) {
            throw new BusinessException("当前检测主流程状态不允许派发任务");
        }

        List<DetectionItem> items = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getRecordId, recordId)
                .orderByAsc(DetectionItem::getCreatedTime));
        if (items.isEmpty()) {
            throw new BusinessException("当前检测主流程缺少参数子流程，无法进行派发");
        }

        LabSample sample = record.getSampleId() == null ? null : labSampleMapper.selectById(record.getSampleId());
        Long orgId = sample == null ? null : resolveSampleOrgId(sample);
        if (orgId == null) {
            orgId = record.getOrgId();
        }
        Map<Long, DetectionItem> itemMap = items.stream()
                .collect(Collectors.toMap(DetectionItem::getId, item -> item));
        List<Long> detectorIds = command.getItems().stream()
                .map(DetectionItemAssignCommand::getDetectorId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        // 子流程支持逐项派人或清空人员，主流程状态会根据全部子流程重新汇总。
        Map<Long, LabUser> detectorMap = loadDetectors(detectorIds, orgId);
        for (DetectionItemAssignCommand itemCommand : command.getItems()) {
            DetectionItem item = itemMap.get(itemCommand.getItemId());
            if (item == null) {
                throw new BusinessException("检测子流程不存在：" + itemCommand.getItemId());
            }
            if (LabWorkflowConstants.DetectionStatus.ENTERED.equals(item.getItemStatus())
                    || LabWorkflowConstants.DetectionStatus.SUBMITTED.equals(item.getItemStatus())
                    || LabWorkflowConstants.DetectionStatus.APPROVED.equals(item.getItemStatus())) {
                continue;
            }
            if (itemCommand.getDetectorId() == null) {
                item.setDetectorId(null);
                item.setDetectorName(null);
                item.setItemStatus(LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN);
            } else {
                LabUser detector = detectorMap.get(itemCommand.getDetectorId());
                if (detector == null) {
                    throw new BusinessException("检测员不存在或已停用：" + itemCommand.getDetectorId());
                }
                if (!businessParticipantService.isDetectionAssignee(detector, orgId)) {
                    throw new BusinessException("检测人员不属于当前样品所属机构，不能分配");
                }
                item.setDetectorId(detector.getId());
                item.setDetectorName(resolveDetectorName(detector));
                item.setItemStatus(LabWorkflowConstants.DetectionStatus.WAIT_DETECT);
                rememberAssignment(orgId, item, detector);
            }
            detectionItemMapper.updateById(item);
        }

        promoteDefaultAssignedItems(items);

        List<DetectionItem> latestItems = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getRecordId, recordId)
                .orderByAsc(DetectionItem::getCreatedTime));
        refreshRecordAssignmentState(record, latestItems);
    }

    /**
     * 为主流程列表批量补充子流程统计摘要。
     *
     * @param records 主流程列表
     */
    public void fillRecordSummaries(List<DetectionRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> recordIds = records.stream()
                .map(DetectionRecord::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (recordIds.isEmpty()) {
            return;
        }
        Map<Long, List<DetectionItem>> itemGroup = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                        .in(DetectionItem::getRecordId, recordIds))
                .stream()
                .collect(Collectors.groupingBy(DetectionItem::getRecordId));

        for (DetectionRecord record : records) {
            List<DetectionItem> recordItems = itemGroup.getOrDefault(record.getId(), Collections.emptyList());
            record.setParameterCount(recordItems.size());
            record.setAssignedCount(countAssignedItems(recordItems));
            record.setCompletedCount(countCompletedItems(recordItems));
            record.setDetectorName(resolveDetectorSummary(recordItems, record.getDetectorName()));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 根据子流程分配情况刷新主流程状态与统计字段。
     *
     * @param record 主流程记录
     * @param items 子流程列表
     */
    public void refreshRecordAssignmentState(DetectionRecord record, List<DetectionItem> items) {
        if (record == null) {
            return;
        }
        List<DetectionItem> effectiveItems = items == null ? new ArrayList<>() : items;
        if (promoteEnteredRecordIfReady(record, null, effectiveItems)) {
            return;
        }
        boolean allAssigned = !effectiveItems.isEmpty() && effectiveItems.stream()
                .allMatch(item -> (item.getDetectorId() != null
                        && !LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN.equals(item.getItemStatus()))
                        || LabWorkflowConstants.DetectionStatus.ENTERED.equals(item.getItemStatus())
                        || LabWorkflowConstants.DetectionStatus.SUBMITTED.equals(item.getItemStatus())
                        || LabWorkflowConstants.DetectionStatus.APPROVED.equals(item.getItemStatus()));
        record.setDetectionStatus(allAssigned
                ? LabWorkflowConstants.DetectionStatus.WAIT_DETECT
                : LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN);

        Set<Long> detectorIds = effectiveItems.stream()
                .map(DetectionItem::getDetectorId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> detectorNames = effectiveItems.stream()
                .map(DetectionItem::getDetectorName)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (detectorIds.size() == 1 && detectorNames.size() == 1) {
            record.setDetectorId(detectorIds.iterator().next());
            record.setDetectorName(detectorNames.iterator().next());
        } else if (detectorIds.size() > 1) {
            record.setDetectorId(null);
            record.setDetectorName("协同检测");
        } else {
            record.setDetectorId(null);
            record.setDetectorName(null);
        }
        detectionRecordMapper.updateById(record);
    }

    private Map<Long, LabUser> loadDetectors(List<Long> detectorIds, Long orgId) {
        if (detectorIds == null || detectorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return labUserMapper.selectList(new LambdaQueryWrapper<LabUser>()
                        .in(LabUser::getId, detectorIds)
                        .eq(LabUser::getStatus, 1))
                .stream()
                .filter(user -> businessParticipantService.isDetectionAssignee(user, orgId))
                .collect(Collectors.toMap(LabUser::getId, user -> user));
    }

    private void promoteDefaultAssignedItems(List<DetectionItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (DetectionItem item : items) {
            if (item == null
                    || item.getDetectorId() == null
                    || !LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN.equals(item.getItemStatus())) {
                continue;
            }
            item.setItemStatus(LabWorkflowConstants.DetectionStatus.WAIT_DETECT);
            detectionItemMapper.updateById(item);
        }
    }

    private void rememberAssignment(Long orgId, DetectionItem item, LabUser detector) {
        if (orgId == null || item == null || item.getParameterId() == null || detector == null || detector.getId() == null) {
            return;
        }
        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                doRememberAssignment(orgId, item, detector);
                return;
            } catch (org.springframework.dao.DeadlockLoserDataAccessException e) {
                if (attempt == maxRetries - 1) {
                    log.warn("记录分配记忆失败（死锁重试耗尽）: orgId={}, parameterId={}", orgId, item.getParameterId());
                    return;
                }
                try {
                    Thread.sleep(50 * (attempt + 1));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                if (isMissingAssignmentMemoryTable(e)) {
                    log.warn("检测分配记忆表不存在，已跳过记忆分配写入: {}", e.getMessage());
                    return;
                }
                log.warn("记录分配记忆失败: {}", e.getMessage());
                return;
            }
        }
    }

    private void doRememberAssignment(Long orgId, DetectionItem item, LabUser detector) {
        DetectionAssignmentMemory memory = detectionAssignmentMemoryMapper.selectOne(new LambdaQueryWrapper<DetectionAssignmentMemory>()
                .eq(DetectionAssignmentMemory::getOrgId, orgId)
                .eq(DetectionAssignmentMemory::getParameterId, item.getParameterId())
                .last("limit 1"));
        if (memory == null) {
            memory = new DetectionAssignmentMemory();
            memory.setOrgId(orgId);
            memory.setParameterId(item.getParameterId());
            memory.setParameterName(item.getParameterName());
            memory.setDetectorId(detector.getId());
            memory.setDetectorName(resolveDetectorName(detector));
            detectionAssignmentMemoryMapper.insert(memory);
            return;
        }
        memory.setParameterName(item.getParameterName());
        memory.setDetectorId(detector.getId());
        memory.setDetectorName(resolveDetectorName(detector));
        detectionAssignmentMemoryMapper.updateById(memory);
    }

    private boolean isMissingAssignmentMemoryTable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            String normalized = message == null ? "" : message.toLowerCase();
            if (normalized.contains("lab_detection_assignment_memory")
                    && (normalized.contains("doesn't exist")
                    || normalized.contains("does not exist")
                    || normalized.contains("not exist")
                    || normalized.contains("\u4e0d\u5b58\u5728"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private List<SampleDetectionConfigItem> parseSampleConfigItems(String snapshotText) {
        if (StrUtil.isBlank(snapshotText)) {
            return new ArrayList<>();
        }
        try {
            List<SampleDetectionConfigItem> items = objectMapper.readValue(
                    snapshotText,
                    new TypeReference<List<SampleDetectionConfigItem>>() {
                    }
            );
            if (items == null) {
                return new ArrayList<>();
            }
            return items.stream()
                    .filter(item -> item != null && item.getParameterId() != null)
                    .collect(Collectors.toList());
        } catch (JsonProcessingException ex) {
            throw new BusinessException("样品检测配置快照格式错误，无法生成检测流程");
        }
    }

    private String resolveDetectionTypeName(LabSample sample) {
        if (sample == null) {
            return null;
        }
        if (StrUtil.isNotBlank(sample.getDetectionTypeName())) {
            return sample.getDetectionTypeName();
        }
        return StrUtil.trim(sample.getDetectionItems());
    }

    private void prefillDetectorIfMissing(DetectionRecord record, LabSample sample) {
        if (record == null || record.getId() == null || sample == null) {
            return;
        }
        List<DetectionItem> items = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getRecordId, record.getId())
                .orderByAsc(DetectionItem::getCreatedTime));
        Long orgId = resolveSampleOrgId(sample);
        if (orgId == null) {
            return;
        }
        List<SampleDetectionConfigItem> configItems = items.stream().map(item -> {
            SampleDetectionConfigItem configItem = new SampleDetectionConfigItem();
            configItem.setParameterId(item.getParameterId());
            configItem.setParameterName(item.getParameterName());
            return configItem;
        }).collect(Collectors.toList());
        Map<Long, DetectionAssignmentMemory> memoryMap = loadAssignmentMemoryMap(orgId, configItems);
        Map<Long, LabUser> memoryDetectorMap = loadMemoryDetectorMap(memoryMap, orgId);
        LabUser fallbackDetector = businessParticipantService.findDefaultDetectionAssignee(orgId);
        boolean changed = false;
        for (DetectionItem item : items) {
            if (item.getDetectorId() != null
                    || !LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN.equals(item.getItemStatus())) {
                continue;
            }
            LabUser initialDetector = resolveInitialDetector(
                    orgId,
                    item.getParameterId(),
                    memoryMap,
                    memoryDetectorMap,
                    fallbackDetector);
            if (initialDetector == null) {
                continue;
            }
            item.setDetectorId(initialDetector.getId());
            item.setDetectorName(resolveDetectorName(initialDetector));
            detectionItemMapper.updateById(item);
            changed = true;
        }
        if (changed) {
            record.setDetectionStatus(LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN);
            detectionRecordMapper.updateById(record);
        }
    }

    private void promoteEnteredRecordIfReady(DetectionRecord record, LabSample sample) {
        if (record == null || record.getId() == null) {
            return;
        }
        List<DetectionItem> items = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getRecordId, record.getId())
                .orderByAsc(DetectionItem::getCreatedTime));
        promoteEnteredRecordIfReady(record, sample, items);
    }

    private boolean promoteEnteredRecordIfReady(DetectionRecord record, LabSample sample, List<DetectionItem> items) {
        if (record == null || items == null || items.isEmpty()) {
            return false;
        }
        boolean allResultsReady = items.stream()
                .allMatch(item -> item.getResultValue() != null
                        && (LabWorkflowConstants.DetectionStatus.ENTERED.equals(item.getItemStatus())
                        || LabWorkflowConstants.DetectionStatus.SUBMITTED.equals(item.getItemStatus())
                        || LabWorkflowConstants.DetectionStatus.APPROVED.equals(item.getItemStatus())));
        if (!allResultsReady) {
            return false;
        }

        for (DetectionItem item : items) {
            if (LabWorkflowConstants.DetectionStatus.ENTERED.equals(item.getItemStatus())) {
                item.setItemStatus(LabWorkflowConstants.DetectionStatus.SUBMITTED);
                detectionItemMapper.updateById(item);
            }
        }

        record.setDetectionStatus(LabWorkflowConstants.DetectionStatus.SUBMITTED);
        detectionRecordMapper.updateById(record);

        LabSample effectiveSample = sample;
        if (effectiveSample == null && record.getSampleId() != null) {
            effectiveSample = labSampleMapper.selectById(record.getSampleId());
        }
        updateSampleStatusForReviewing(effectiveSample);
        return true;
    }

    private void updateSampleStatusForReviewing(LabSample sample) {
        if (sample == null || sample.getId() == null) {
            return;
        }
        if (LabWorkflowConstants.SampleStatus.COMPLETED.equals(sample.getSampleStatus())) {
            return;
        }
        // 直接更新样品状态（此处不经过 LabSampleService 以避免循环依赖）
        // 状态流转校验已在调用方完成
        sample.setSampleStatus(LabWorkflowConstants.SampleStatus.REVIEWING);
        labSampleMapper.updateById(sample);
    }

    private String resolveDetectorName(LabUser detector) {
        if (detector == null) {
            return null;
        }
        return StrUtil.isNotBlank(detector.getRealName()) ? detector.getRealName() : detector.getUsername();
    }

    private int countAssignedItems(List<DetectionItem> items) {
        return (int) (items == null ? 0L : items.stream()
                .filter(item -> item.getDetectorId() != null)
                .count());
    }

    private int countCompletedItems(List<DetectionItem> items) {
        return (int) (items == null ? 0L : items.stream()
                .filter(item -> LabWorkflowConstants.DetectionStatus.ENTERED.equals(item.getItemStatus())
                        || LabWorkflowConstants.DetectionStatus.SUBMITTED.equals(item.getItemStatus())
                        || LabWorkflowConstants.DetectionStatus.APPROVED.equals(item.getItemStatus()))
                .count());
    }

    private String resolveDetectorSummary(List<DetectionItem> items, String fallbackDetectorName) {
        if (items == null || items.isEmpty()) {
            return fallbackDetectorName;
        }
        Set<String> detectorNames = items.stream()
                .map(DetectionItem::getDetectorName)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!detectorNames.isEmpty()) {
            return String.join("、", detectorNames);
        }
        return StrUtil.trim(fallbackDetectorName);
    }

    private void softDeleteRecord(DetectionRecord record, List<DetectionItem> items) {
        if (record == null) {
            return;
        }
        record.setDeleted(1);
        detectionRecordMapper.updateById(record);
        if (items == null || items.isEmpty()) {
            return;
        }
        for (DetectionItem item : items) {
            item.setDeleted(1);
            detectionItemMapper.updateById(item);
        }
    }
}
