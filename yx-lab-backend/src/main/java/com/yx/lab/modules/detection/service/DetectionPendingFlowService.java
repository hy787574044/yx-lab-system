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
import com.yx.lab.modules.detection.entity.DetectionItem;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionItemMapper;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.sample.dto.SampleDetectionConfigItem;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.system.entity.LabUser;
import com.yx.lab.modules.system.mapper.LabUserMapper;
import lombok.RequiredArgsConstructor;
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
public class DetectionPendingFlowService {

    private static final String DETECTOR_ROLE_CODE = "DETECTOR";

    /**
     * 按样品维度保存并发锁，避免页面派发与自动补偿同时生成重复主流程。
     */
    private final ConcurrentMap<Long, Object> sampleLocks = new ConcurrentHashMap<>();

    private final DetectionRecordMapper detectionRecordMapper;

    private final DetectionItemMapper detectionItemMapper;

    private final LabSampleMapper labSampleMapper;

    private final LabUserMapper labUserMapper;

    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    /**
     * 扫描所有处于待检范围内的样品，并补齐待分配检测主流程。
     */
    public void syncPendingFlowsForOpenSamples() {
        List<LabSample> samples = labSampleMapper.selectList(new LambdaQueryWrapper<LabSample>()
                .in(LabSample::getSampleStatus, LabWorkflowConstants.DETECTABLE_SAMPLE_STATUSES)
                .orderByAsc(LabSample::getCreatedTime));
        for (LabSample sample : samples) {
            createPendingFlowIfMissing(sample);
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
                return existing;
            }

            // 样品登录时冻结的套餐参数快照，是后续生成参数子流程的唯一数据来源。
            List<SampleDetectionConfigItem> configItems = parseSampleConfigItems(sample.getDetectionConfigSnapshot());
            if (configItems.isEmpty()) {
                return null;
            }

            DetectionRecord record = new DetectionRecord();
            record.setSampleId(sample.getId());
            record.setSampleNo(sample.getSampleNo());
            record.setSealNo(sample.getSealNo());
            record.setDetectionTypeId(sample.getDetectionTypeId());
            record.setDetectionTypeName(resolveDetectionTypeName(sample));
            record.setDetectionTime(sample.getSealTime() != null ? sample.getSealTime() : LocalDateTime.now());
            record.setDetectorId(null);
            record.setDetectorName(null);
            record.setDetectionResult(null);
            record.setAbnormalRemark("待分配检测员");
            record.setDetectionStatus(LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN);
            detectionRecordMapper.insert(record);

            // 每一个套餐参数都展开成独立子流程，后续可单独分配检测员、录入结果和审查。
            for (SampleDetectionConfigItem configItem : configItems) {
                DetectionItem item = new DetectionItem();
                item.setRecordId(record.getId());
                item.setParameterId(configItem.getParameterId());
                item.setParameterName(configItem.getParameterName());
                item.setStandardMin(configItem.getStandardMin());
                item.setStandardMax(configItem.getStandardMax());
                item.setUnit(configItem.getUnit());
                item.setReferenceStandard(configItem.getReferenceStandard());
                item.setMethodId(configItem.getMethodId());
                item.setMethodName(configItem.getMethodName());
                item.setDetectorId(null);
                item.setDetectorName(null);
                item.setItemStatus(LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN);
                item.setExceedFlag(0);
                detectionItemMapper.insert(item);
            }
            return record;
        }
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

        Map<Long, DetectionItem> itemMap = items.stream()
                .collect(Collectors.toMap(DetectionItem::getId, item -> item));
        List<Long> detectorIds = command.getItems().stream()
                .map(DetectionItemAssignCommand::getDetectorId)
                .filter(id -> id != null)
                .distinct()
        // 子流程支持逐项派人或清空人员，主流程状态会根据全部子流程重新汇总。
        Map<Long, LabUser> detectorMap = loadDetectors(detectorIds);

        // 子流程支持逐项派人或清空人员，主流程状态会根据全部子流程重新汇总。
        for (DetectionItemAssignCommand itemCommand : command.getItems()) {
            DetectionItem item = itemMap.get(itemCommand.getItemId());
            if (item == null) {
                throw new BusinessException("检测子流程不存在：" + itemCommand.getItemId());
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
                item.setDetectorId(detector.getId());
                item.setDetectorName(resolveDetectorName(detector));
                item.setItemStatus(LabWorkflowConstants.DetectionStatus.WAIT_DETECT);
            }
            detectionItemMapper.updateById(item);
        }

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
        boolean allAssigned = !effectiveItems.isEmpty() && effectiveItems.stream().allMatch(item -> item.getDetectorId() != null);
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

    private Map<Long, LabUser> loadDetectors(List<Long> detectorIds) {
        if (detectorIds == null || detectorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return labUserMapper.selectList(new LambdaQueryWrapper<LabUser>()
                        .in(LabUser::getId, detectorIds)
                        .eq(LabUser::getStatus, 1)
                        .eq(LabUser::getRoleCode, DETECTOR_ROLE_CODE))
                .stream()
                .collect(Collectors.toMap(LabUser::getId, user -> user));
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
                .filter(item -> LabWorkflowConstants.DetectionStatus.SUBMITTED.equals(item.getItemStatus())
                        || LabWorkflowConstants.DetectionStatus.APPROVED.equals(item.getItemStatus()))
                .count());
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
