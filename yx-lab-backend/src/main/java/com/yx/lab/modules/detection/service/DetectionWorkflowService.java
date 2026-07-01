package com.yx.lab.modules.detection.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.DataScopeHelper;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.detection.dto.DetectionAssignCommand;
import com.yx.lab.modules.detection.dto.DetectionItemCommand;
import com.yx.lab.modules.detection.dto.DetectionItemQuery;
import com.yx.lab.modules.detection.dto.DetectionRecordQuery;
import com.yx.lab.modules.detection.dto.DetectionSubmitCommand;
import com.yx.lab.modules.detection.entity.DetectionItem;
import com.yx.lab.modules.detection.entity.DetectionMethod;
import com.yx.lab.modules.detection.entity.DetectionParameter;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.entity.DetectionType;
import com.yx.lab.modules.detection.mapper.DetectionItemMapper;
import com.yx.lab.modules.detection.mapper.DetectionMethodMapper;
import com.yx.lab.modules.detection.mapper.DetectionParameterMapper;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.detection.mapper.DetectionTypeMapper;
import com.yx.lab.modules.detection.vo.DetectionRecordDetailVO;
import com.yx.lab.modules.detection.vo.DetectionItemPageVO;
import com.yx.lab.modules.detection.vo.DetectionItemSummaryVO;
import com.yx.lab.modules.detection.vo.DetectionRecordSummaryVO;
import com.yx.lab.modules.review.entity.ReviewRecord;
import com.yx.lab.modules.review.mapper.ReviewRecordMapper;
import com.yx.lab.modules.sample.dto.SampleDetectionConfigItem;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.sample.service.LabSampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 检测流程服务。
 */
@Service
@RequiredArgsConstructor
public class DetectionWorkflowService {

    private final DetectionRecordMapper detectionRecordMapper;

    private final DetectionItemMapper detectionItemMapper;

    private final DetectionMethodMapper detectionMethodMapper;

    private final DetectionTypeMapper detectionTypeMapper;

    private final DetectionParameterMapper detectionParameterMapper;

    private final LabSampleMapper labSampleMapper;

    private final ReviewRecordMapper reviewRecordMapper;

    private final LabSampleService labSampleService;

    private final DetectionPendingFlowService detectionPendingFlowService;

    private final ObjectMapper objectMapper;

    private final DataScopeHelper dataScopeHelper;

    /**
     * 分页查询检测主流程列表。
     *
     * @param query 查询条件
     * @return 检测主流程分页结果
     */
    public PageResult<DetectionRecord> page(DetectionRecordQuery query) {
        if (shouldSyncPendingFlows(query)) {
            detectionPendingFlowService.syncPendingFlowsForOpenSamples();
        }
        Page<DetectionRecord> page = detectionRecordMapper.selectPage(
                PageUtils.buildPage(query),
                buildRecordQueryWrapper(query, false, true));
        detectionPendingFlowService.fillRecordSummaries(page.getRecords());
        fillRecordSampleInfo(page.getRecords());
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public DetectionRecordSummaryVO summary(DetectionRecordQuery query) {
        DetectionRecordSummaryVO summary = new DetectionRecordSummaryVO();
        summary.setTotal(countRecords(query, null));
        summary.setWaitAssignCount(countRecords(query, LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN));
        summary.setWaitDetectCount(countRecords(query, LabWorkflowConstants.DetectionStatus.WAIT_DETECT));
        summary.setPendingReviewCount(countRecords(query, LabWorkflowConstants.DetectionStatus.SUBMITTED));
        summary.setApprovedCount(countRecords(query, LabWorkflowConstants.DetectionStatus.APPROVED));
        summary.setRejectedCount(countRecords(query, LabWorkflowConstants.DetectionStatus.REJECTED));
        return summary;
    }

    public PageResult<DetectionItemPageVO> itemPage(DetectionItemQuery query) {
        if (shouldSyncPendingFlows(query)) {
            detectionPendingFlowService.syncPendingFlowsForOpenSamples();
        }
        Page<DetectionItem> page = detectionItemMapper.selectPage(
                PageUtils.buildPage(query),
                buildItemQueryWrapper(query, false, true));
        return new PageResult<>(page.getTotal(), buildDetectionItemPageList(page.getRecords()));
    }

    private boolean shouldSyncPendingFlows(DetectionRecordQuery query) {
        String status = query == null ? null : query.getDetectionStatus();
        String scope = query == null ? null : query.getScope();
        if (StrUtil.equals(scope, "detection-split")) {
            return true;
        }
        if (StrUtil.isBlank(status)) {
            return true;
        }
        return LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN.equals(status)
                || LabWorkflowConstants.DetectionStatus.WAIT_DETECT.equals(status);
    }

    private boolean shouldSyncPendingFlows(DetectionItemQuery query) {
        String status = query == null ? null : query.getItemStatus();
        if (StrUtil.isBlank(status)) {
            return true;
        }
        return LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN.equals(status)
                || LabWorkflowConstants.DetectionStatus.WAIT_DETECT.equals(status);
    }

    public DetectionItemSummaryVO itemSummary(DetectionItemQuery query) {
        DetectionItemSummaryVO summary = new DetectionItemSummaryVO();
        summary.setTotal(countItems(query, null));
        summary.setWaitAssignCount(countItems(query, LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN));
        summary.setWaitDetectCount(countItems(query, LabWorkflowConstants.DetectionStatus.WAIT_DETECT));
        summary.setPendingReviewCount(countItems(query, LabWorkflowConstants.DetectionStatus.SUBMITTED));
        summary.setApprovedCount(countItems(query, LabWorkflowConstants.DetectionStatus.APPROVED));
        summary.setRejectedCount(countItems(query, LabWorkflowConstants.DetectionStatus.REJECTED));
        return summary;
    }

    /**
     * 获取检测主流程详情及子流程明细。
     *
     * @param id 检测主流程ID
     * @return 检测主流程详情
     */
    public DetectionRecordDetailVO detail(Long id) {
        DetectionRecord record = detectionRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("检测主流程不存在");
        }
        List<DetectionItem> items = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getRecordId, id)
                .orderByAsc(DetectionItem::getCreatedTime));
        fillMethodBasis(items);
        DetectionRecordDetailVO vo = new DetectionRecordDetailVO();
        vo.setRecord(record);
        vo.setItems(items);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 对检测主流程执行参数级人员分配。
     *
     * @param recordId 检测主流程ID
     * @param command 分配参数
     */
    public void assignDetectors(Long recordId, DetectionAssignCommand command) {
        detectionPendingFlowService.assignDetectors(recordId, command);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 提交检测结果，可按子流程逐条录入并更新主流程汇总状态。
     *
     * @param command 检测结果提交参数
     */
    public void submit(DetectionSubmitCommand command) {
        LabSample sample = labSampleMapper.selectById(command.getSampleId());
        if (sample == null) {
            throw new BusinessException("样品不存在");
        }
        if (!LabWorkflowConstants.canSubmitDetection(sample.getSampleStatus())) {
            throw new BusinessException("当前样品状态不允许提交检测结果");
        }
        if (LabWorkflowConstants.SampleStatus.RETEST.equals(sample.getSampleStatus())) {
            validateRetestSubmission(sample);
        }

        // 检测提交始终以样品登录时冻结的套餐配置和参数快照为准，不直接相信前端传参。
        DetectionType detectionType = detectionTypeMapper.selectById(command.getDetectionTypeId());
        DetectionType usableType = requireUsableType(command, detectionType);
        List<DetectionParameter> configuredParameters = resolveConfiguredParameters(usableType, sample);

        CurrentUser currentUser = requireCurrentUser();
        DetectionRecord activeRecord = resolvePendingRecord(command, sample);
        if (activeRecord != null) {
            validateSubmitRecordOwnership(activeRecord, sample);
        }

        // 若样品当前还处于待分配或待检测主流程，则走参数子流程逐条提交；否则按旧模式整单提交。
        if (activeRecord != null && LabWorkflowConstants.canAssignDetection(activeRecord.getDetectionStatus())) {
            submitPendingRecord(
                    activeRecord,
                    sample,
                    usableType,
                    configuredParameters,
                    command.getItems(),
                    currentUser,
                    command.getAbnormalRemark(),
                    command.getRemark(),
                    command.getItemId());
            return;
        }

        Map<Long, DetectionItemCommand> itemMap = validateSubmittedItems(command.getItems(), configuredParameters);
        Number pendingCount = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getSampleId, sample.getId())
                .eq(DetectionRecord::getDetectionStatus, LabWorkflowConstants.DetectionStatus.SUBMITTED));
        if (pendingCount != null && pendingCount.longValue() > 0) {
            throw new BusinessException("当前样品已存在已提交的检测记录，请勿重复提交");
        }
        validateDetectorBinding(usableType, currentUser);
        insertSubmittedRecord(sample, usableType, configuredParameters, itemMap, currentUser, command.getAbnormalRemark(), command.getRemark());
    }

    private DetectionRecord resolvePendingRecord(DetectionSubmitCommand command, LabSample sample) {
        if (command.getRecordId() != null) {
            DetectionRecord record = detectionRecordMapper.selectById(command.getRecordId());
            if (record == null) {
                throw new BusinessException("检测主流程不存在");
            }
            return record;
        }
        DetectionRecord activeRecord = detectionPendingFlowService.createPendingFlowIfMissing(sample);
        if (activeRecord == null) {
            activeRecord = detectionPendingFlowService.findActiveRecordBySampleId(sample.getId());
        }
        return activeRecord;
    }

    private void validateSubmitRecordOwnership(DetectionRecord record, LabSample sample) {
        if (record == null || sample == null) {
            return;
        }
        if (record.getSampleId() == null || !record.getSampleId().equals(sample.getId())) {
            throw new BusinessException("当前检测主流程与样品不匹配，请刷新后重试");
        }
    }

    private void submitPendingRecord(DetectionRecord record,
                                     LabSample sample,
                                     DetectionType detectionType,
                                     List<DetectionParameter> configuredParameters,
                                     List<DetectionItemCommand> submittedItems,
                                     CurrentUser currentUser,
                                     String abnormalRemark,
                                     String remark,
                                     Long expectedItemId) {
        List<DetectionItem> pendingItems = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getRecordId, record.getId())
                .orderByAsc(DetectionItem::getCreatedTime));
        // 参数子流程按 parameterId 建索引，确保单参数录入时只更新自己负责的那一条子流程。
        Map<Long, DetectionItem> pendingItemMap = pendingItems.stream()
                .collect(Collectors.toMap(DetectionItem::getParameterId, item -> item, (left, right) -> left));
        Map<Long, DetectionItemCommand> itemMap = validatePendingSubmittedItems(
                submittedItems,
                configuredParameters,
                pendingItemMap,
                currentUser,
                expectedItemId);
        Map<Long, DetectionParameter> parameterMap = configuredParameters.stream()
                .collect(Collectors.toMap(DetectionParameter::getId, parameter -> parameter, (left, right) -> left));

        // 每次提交只覆盖本次录入的参数结果，未提交的子流程保持原状态等待继续录入。
        for (Map.Entry<Long, DetectionItemCommand> entry : itemMap.entrySet()) {
            DetectionParameter parameter = parameterMap.get(entry.getKey());
            DetectionItem pendingItem = pendingItemMap.get(entry.getKey());
            DetectionItemCommand itemCommand = entry.getValue();
            pendingItem.setParameterName(parameter.getParameterName());
            pendingItem.setStandardMin(parameter.getStandardMin());
            pendingItem.setStandardMax(parameter.getStandardMax());
            pendingItem.setOptionValues(parameter.getOptionValues());
            pendingItem.setUnit(parameter.getUnit());
            pendingItem.setResultValue(itemCommand.getResultValue());
            pendingItem.setExceedFlag(isExceeded(parameter, itemCommand.getResultValue()) ? 1 : 0);
            pendingItem.setItemStatus(LabWorkflowConstants.DetectionStatus.SUBMITTED);
            detectionItemMapper.updateById(pendingItem);
        }

        record.setDetectionTypeId(detectionType.getId());
        record.setDetectionTypeName(detectionType.getTypeName());
        record.setDetectionTime(LocalDateTime.now());
        if (StrUtil.isNotBlank(abnormalRemark)) {
            record.setAbnormalRemark(StrUtil.trim(abnormalRemark));
        }
        record.setRemark(StrUtil.trim(remark));
        // 主流程的检测员、状态和结果，统一根据全部子流程的最新状态重新汇总。
        applyRecordDetectorSummary(record, pendingItems, currentUser);
        applyPendingRecordStatus(record, sample, pendingItems, currentUser);
        detectionRecordMapper.updateById(record);
    }

    private LambdaQueryWrapper<DetectionRecord> buildRecordQueryWrapper(DetectionRecordQuery query,
                                                                        boolean ignoreStatusFilter,
                                                                        boolean withOrder) {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        String keyword = query == null ? null : StrUtil.trim(query.getKeyword());
        String status = query == null ? null : query.getDetectionStatus();
        String scope = query == null ? null : query.getScope();
        Boolean mine = query == null ? null : query.getMine();
        Long scopedDetectorId = resolveScopedDetectorId(currentUser, mine);
        LambdaQueryWrapper<DetectionRecord> wrapper = new LambdaQueryWrapper<DetectionRecord>()
                .and(StrUtil.isNotBlank(keyword), condition -> condition
                        .like(DetectionRecord::getSampleNo, keyword)
                        .or()
                        .like(DetectionRecord::getDetectionTypeName, keyword))
                .eq(scopedDetectorId != null,
                        DetectionRecord::getDetectorId,
                        scopedDetectorId);
        if (!ignoreStatusFilter) {
            if (StrUtil.isNotBlank(status)) {
                wrapper.eq(DetectionRecord::getDetectionStatus, status);
            } else {
                applyRecordScope(wrapper, scope);
            }
        }
        if (withOrder) {
            wrapper.orderByDesc(DetectionRecord::getDetectionTime)
                    .orderByDesc(DetectionRecord::getCreatedTime);
        }
        return wrapper;
    }

    private void applyRecordScope(LambdaQueryWrapper<DetectionRecord> wrapper, String scope) {
        if (StrUtil.equals(scope, "detection-split")) {
            // 检测分样：待分配 + 待检测 + 待审查
            wrapper.in(DetectionRecord::getDetectionStatus,
                    LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN,
                    LabWorkflowConstants.DetectionStatus.WAIT_DETECT,
                    LabWorkflowConstants.DetectionStatus.SUBMITTED);
            return;
        }
        if (StrUtil.equals(scope, "detection-analysis")) {
            // 检测分析：待分配 + 待检测 + 待审查（与 detection-split 一致）
            wrapper.in(DetectionRecord::getDetectionStatus,
                    LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN,
                    LabWorkflowConstants.DetectionStatus.WAIT_DETECT,
                    LabWorkflowConstants.DetectionStatus.SUBMITTED);
            return;
        }
        if (StrUtil.equals(scope, "detection-history")) {
            // 历史检测：已通过 + 已驳回
            wrapper.in(DetectionRecord::getDetectionStatus,
                    LabWorkflowConstants.DetectionStatus.APPROVED,
                    LabWorkflowConstants.DetectionStatus.REJECTED);
        }
    }

    private long countRecords(DetectionRecordQuery query, String status) {
        LambdaQueryWrapper<DetectionRecord> wrapper = buildRecordQueryWrapper(query, true, false);
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(DetectionRecord::getDetectionStatus, status);
        }
        // 始终应用 scope 过滤，保持统计与列表一致
        String scope = query == null ? null : query.getScope();
        if (StrUtil.isNotBlank(scope)) {
            applyRecordScope(wrapper, scope);
        }
        Number count = detectionRecordMapper.selectCount(wrapper);
        return count == null ? 0L : count.longValue();
    }

    private long countItems(DetectionItemQuery query, String status) {
        LambdaQueryWrapper<DetectionItem> wrapper = buildItemQueryWrapper(query, true, false);
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(DetectionItem::getItemStatus, status);
        }
        Number count = detectionItemMapper.selectCount(wrapper);
        return count == null ? 0L : count.longValue();
    }

    private LambdaQueryWrapper<DetectionItem> buildItemQueryWrapper(DetectionItemQuery query, boolean ignoreStatusFilter) {
        return buildItemQueryWrapper(query, ignoreStatusFilter, true);
    }

    private LambdaQueryWrapper<DetectionItem> buildItemQueryWrapper(DetectionItemQuery query,
                                                                    boolean ignoreStatusFilter,
                                                                    boolean withOrder) {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        String keyword = query == null ? null : StrUtil.trim(query.getKeyword());
        String itemStatus = query == null ? null : query.getItemStatus();
        Boolean mine = query == null ? null : query.getMine();
        List<Long> matchedRecordIds = findMatchedRecordIds(keyword);
        Long scopedDetectorId = resolveScopedDetectorId(currentUser, mine);
        LambdaQueryWrapper<DetectionItem> wrapper = new LambdaQueryWrapper<DetectionItem>()
                .eq(StrUtil.isNotBlank(itemStatus) && !ignoreStatusFilter,
                        DetectionItem::getItemStatus,
                        itemStatus)
                .eq(scopedDetectorId != null,
                        DetectionItem::getDetectorId,
                        scopedDetectorId);
        if (withOrder) {
            wrapper.orderByDesc(DetectionItem::getUpdatedTime)
                    .orderByDesc(DetectionItem::getCreatedTime);
        }
        if (StrUtil.isBlank(keyword)) {
            return wrapper;
        }
        wrapper.and(condition -> {
            condition.like(DetectionItem::getParameterName, keyword)
                    .or()
                    .like(DetectionItem::getMethodName, keyword)
                    .or()
                    .like(DetectionItem::getDetectorName, keyword);
            if (!matchedRecordIds.isEmpty()) {
                condition.or().in(DetectionItem::getRecordId, matchedRecordIds);
            }
        });
        return wrapper;
    }

    private List<Long> findMatchedRecordIds(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return Collections.emptyList();
        }
        return detectionRecordMapper.selectList(new LambdaQueryWrapper<DetectionRecord>()
                        .select(DetectionRecord::getId)
                        .and(wrapper -> wrapper
                                .like(DetectionRecord::getSampleNo, keyword)
                                .or()
                                .like(DetectionRecord::getDetectionTypeName, keyword)))
                .stream()
                .map(DetectionRecord::getId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
    }

    private Long resolveScopedDetectorId(CurrentUser currentUser, Boolean mine) {
        // 明确选择"仅看我的"
        if (Boolean.TRUE.equals(mine) && currentUser != null) {
            return currentUser.getUserId();
        }
        // STAFF 角色默认只能看自己的（无论 mine 是 null 还是 false）
        if (dataScopeHelper.isRole("STAFF") && currentUser != null) {
            return currentUser.getUserId();
        }
        // ADMIN/DIRECTOR 看全部
        return null;
    }

    private List<DetectionItemPageVO> buildDetectionItemPageList(List<DetectionItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        fillMethodBasis(items);
        Map<Long, DetectionRecord> recordMap = loadDetectionRecordMap(items);
        Map<Long, LabSample> sampleMap = loadSampleMap(recordMap.values());
        return items.stream().map(item -> {
            DetectionRecord record = recordMap.get(item.getRecordId());
            LabSample sample = resolveRecordSample(record, sampleMap);
            DetectionItemPageVO vo = new DetectionItemPageVO();
            vo.setId(item.getId());
            vo.setRecordId(item.getRecordId());
            vo.setSampleId(record == null ? null : record.getSampleId());
            vo.setSampleNo(record == null ? null : record.getSampleNo());
            vo.setPointName(sample == null ? null : sample.getPointName());
            vo.setSampleType(sample == null ? null : sample.getSampleType());
            vo.setSampleTypeLabel(sample == null ? null : LabWorkflowConstants.getSampleTypeLabel(sample.getSampleType()));
            vo.setSampleSourceMethod(sample == null ? null : sample.getSampleSourceMethod());
            vo.setSampleSourceMethodLabel(sample == null ? null : LabWorkflowConstants.getSampleSourceMethodLabel(sample.getSampleSourceMethod()));
            vo.setDetectionTypeId(record == null ? null : record.getDetectionTypeId());
            vo.setDetectionTypeName(record == null ? null : record.getDetectionTypeName());
            vo.setDetectionTime(record == null ? null : record.getDetectionTime());
            vo.setParameterId(item.getParameterId());
            vo.setParameterName(item.getParameterName());
            vo.setStandardMin(item.getStandardMin());
            vo.setStandardMax(item.getStandardMax());
            vo.setOptionValues(item.getOptionValues());
            vo.setResultValue(item.getResultValue());
            vo.setUnit(item.getUnit());
            vo.setReferenceStandard(item.getReferenceStandard());
            vo.setMethodId(item.getMethodId());
            vo.setMethodName(item.getMethodName());
            vo.setMethodBasis(item.getMethodBasis());
            vo.setDetectorId(item.getDetectorId());
            vo.setDetectorName(item.getDetectorName());
            vo.setItemStatus(item.getItemStatus());
            vo.setItemStatusDesc(LabWorkflowConstants.getDetectionStatusLabel(item.getItemStatus()));
            vo.setExceedFlag(item.getExceedFlag());
            vo.setAbnormalRemark(record == null ? null : record.getAbnormalRemark());
            vo.setRemark(record == null ? null : record.getRemark());
            vo.setUpdatedTime(item.getUpdatedTime());
            return vo;
        }).collect(Collectors.toList());
    }

    private void fillRecordSampleInfo(List<DetectionRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<Long, LabSample> sampleMap = loadSampleMap(records);
        for (DetectionRecord record : records) {
            LabSample sample = resolveRecordSample(record, sampleMap);
            if (sample == null) {
                continue;
            }
            record.setPointName(sample.getPointName());
            record.setSampleType(sample.getSampleType());
            record.setSampleTypeLabel(LabWorkflowConstants.getSampleTypeLabel(sample.getSampleType()));
        }
    }

    private Map<Long, DetectionRecord> loadDetectionRecordMap(List<DetectionItem> items) {
        List<Long> recordIds = items.stream()
                .map(DetectionItem::getRecordId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (recordIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return detectionRecordMapper.selectList(new LambdaQueryWrapper<DetectionRecord>()
                        .in(DetectionRecord::getId, recordIds))
                .stream()
                .collect(Collectors.toMap(DetectionRecord::getId, item -> item, (left, right) -> left));
    }

    private Map<Long, LabSample> loadSampleMap(java.util.Collection<DetectionRecord> records) {
        List<Long> sampleIds = records.stream()
                .filter(record -> record != null && record.getSampleId() != null)
                .map(DetectionRecord::getSampleId)
                .distinct()
                .collect(Collectors.toList());
        if (sampleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return labSampleMapper.selectList(new LambdaQueryWrapper<LabSample>()
                        .in(LabSample::getId, sampleIds))
                .stream()
                .collect(Collectors.toMap(LabSample::getId, item -> item, (left, right) -> left));
    }

    private LabSample resolveRecordSample(DetectionRecord record, Map<Long, LabSample> sampleMap) {
        if (record == null) {
            return null;
        }
        LabSample sample = record.getSampleId() == null ? null : sampleMap.get(record.getSampleId());
        if (sample != null || StrUtil.isBlank(record.getSampleNo())) {
            return sample;
        }
        return labSampleMapper.selectOne(new LambdaQueryWrapper<LabSample>()
                .eq(LabSample::getSampleNo, record.getSampleNo())
                .last("limit 1"));
    }

    private void insertSubmittedRecord(LabSample sample,
                                       DetectionType detectionType,
                                       List<DetectionParameter> configuredParameters,
                                       Map<Long, DetectionItemCommand> itemMap,
                                       CurrentUser currentUser,
                                       String abnormalRemark,
                                       String remark) {
        // 兼容旧的整单提交模式：直接生成一条主流程和完整的参数结果明细。
        DetectionRecord record = new DetectionRecord();
        record.setSampleId(sample.getId());
        record.setSampleNo(sample.getSampleNo());
        record.setDetectionTypeId(detectionType.getId());
        record.setDetectionTypeName(detectionType.getTypeName());
        record.setDetectionTime(LocalDateTime.now());
        record.setDetectorId(currentUser.getUserId());
        record.setDetectorName(currentUser.getRealName());
        record.setAbnormalRemark(StrUtil.trim(abnormalRemark));
        record.setRemark(StrUtil.trim(remark));
        record.setDetectionStatus(LabWorkflowConstants.DetectionStatus.SUBMITTED);
        record.setDetectionResult(buildResult(configuredParameters, itemMap));
        detectionRecordMapper.insert(record);

        for (DetectionParameter parameter : configuredParameters) {
            DetectionItemCommand itemCommand = itemMap.get(parameter.getId());
            DetectionItem item = new DetectionItem();
            item.setRecordId(record.getId());
            item.setParameterId(parameter.getId());
            item.setParameterName(parameter.getParameterName());
            item.setStandardMin(parameter.getStandardMin());
            item.setStandardMax(parameter.getStandardMax());
            item.setOptionValues(parameter.getOptionValues());
            item.setResultValue(itemCommand.getResultValue());
            item.setUnit(parameter.getUnit());
            item.setItemStatus(LabWorkflowConstants.DetectionStatus.SUBMITTED);
            item.setExceedFlag(isExceeded(parameter, itemCommand.getResultValue()) ? 1 : 0);
            detectionItemMapper.insert(item);
        }

        labSampleService.updateStatus(
                sample.getId(),
                LabWorkflowConstants.SampleStatus.REVIEWING,
                record.getDetectionResult(),
                "检测结果已提交：样品编号=" + sample.getSampleNo()
                        + "，检测套餐=" + record.getDetectionTypeName()
                        + "，检测员=" + currentUser.getRealName()
                        + "，结果=" + LabWorkflowConstants.getDetectionResultLabel(record.getDetectionResult()));
    }

    private void validateRetestSubmission(LabSample sample) {
        ReviewRecord latestReview = reviewRecordMapper.selectOne(new LambdaQueryWrapper<ReviewRecord>()
                .eq(ReviewRecord::getSampleId, sample.getId())
                .orderByDesc(ReviewRecord::getReviewTime)
                .orderByDesc(ReviewRecord::getCreatedTime)
                .last("limit 1"));
        if (latestReview == null) {
            throw new BusinessException("当前样品缺少最近一次审核记录，无法按退回重检流程提交");
        }
        if (!LabWorkflowConstants.ReviewResult.REJECTED.equals(latestReview.getReviewResult())) {
            throw new BusinessException("当前样品最近一次审核结果不是驳回状态，不能按退回重检流程提交");
        }
    }

    private DetectionType requireUsableType(DetectionSubmitCommand command, DetectionType detectionType) {
        if (detectionType == null) {
            throw new BusinessException("检测套餐不存在");
        }
        if (!Integer.valueOf(1).equals(detectionType.getEnabled())) {
            throw new BusinessException("当前检测套餐已停用，请选择其他检测套餐");
        }
        if (StrUtil.isNotBlank(command.getDetectionTypeName())
                && !StrUtil.equals(command.getDetectionTypeName(), detectionType.getTypeName())) {
            throw new BusinessException("检测套餐名称与实际配置不一致，请刷新页面后重试");
        }
        return detectionType;
    }

    private void validateDetectorBinding(DetectionType detectionType, CurrentUser currentUser) {
        if (detectionType.getDetectorId() == null) {
            return;
        }
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("当前登录用户信息失效，请重新登录后再提交");
        }
        if (hasFullAccessRole(currentUser)) {
            return;
        }
        if (!detectionType.getDetectorId().equals(currentUser.getUserId())) {
            throw new BusinessException("当前检测套餐已指定检测员“" + detectionType.getDetectorName() + "”，您无权提交该检测结果");
        }
    }

    private List<DetectionParameter> resolveConfiguredParameters(DetectionType detectionType, LabSample sample) {
        List<Long> parameterIds = resolveSampleParameterIds(sample, detectionType);
        if (parameterIds.isEmpty()) {
            throw new BusinessException("当前检测套餐未配置检测参数，请先完善套餐配置");
        }
        List<DetectionParameter> parameters = detectionParameterMapper.selectList(new LambdaQueryWrapper<DetectionParameter>()
                .in(DetectionParameter::getId, parameterIds));
        Map<Long, DetectionParameter> parameterMap = parameters.stream()
                .collect(Collectors.toMap(DetectionParameter::getId, parameter -> parameter));
        List<DetectionParameter> orderedParameters = new ArrayList<>();
        for (Long parameterId : parameterIds) {
            DetectionParameter parameter = parameterMap.get(parameterId);
            if (parameter == null) {
                throw new BusinessException("检测套餐中存在已删除的检测参数，请检查套餐配置");
            }
            if (!Integer.valueOf(1).equals(parameter.getEnabled())) {
                throw new BusinessException("检测套餐中存在已停用的检测参数，请先调整套餐后再提交");
            }
            orderedParameters.add(parameter);
        }
        return orderedParameters;
    }


    private List<Long> resolveSampleParameterIds(LabSample sample, DetectionType detectionType) {
        List<Long> parameterIds = parseSampleDetectionConfigParameterIds(sample == null ? null : sample.getDetectionConfigSnapshot());
        if (!parameterIds.isEmpty()) {
            return parameterIds;
        }
        return parseParameterIds(detectionType.getParameterIds());
    }

    private List<Long> parseSampleDetectionConfigParameterIds(String snapshotText) {
        if (StrUtil.isBlank(snapshotText)) {
            return new ArrayList<>();
        }
        try {
            List<SampleDetectionConfigItem> items = objectMapper.readValue(
                    snapshotText,
                    new TypeReference<List<SampleDetectionConfigItem>>() {
                    }
            );
            if (items == null || items.isEmpty()) {
                return new ArrayList<>();
            }
            return items.stream()
                    .map(SampleDetectionConfigItem::getParameterId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (JsonProcessingException ex) {
            throw new BusinessException("样品检测配置快照格式错误，无法解析检测参数");
        }
    }

    private Map<Long, DetectionItemCommand> validateSubmittedItems(List<DetectionItemCommand> items,
                                                                   List<DetectionParameter> configuredParameters) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("检测结果明细不能为空");
        }
        Map<Long, DetectionItemCommand> itemMap = new LinkedHashMap<>();
        for (DetectionItemCommand item : items) {
            if (itemMap.put(item.getParameterId(), item) != null) {
                throw new BusinessException("检测结果明细中存在重复的检测参数");
            }
        }
        if (itemMap.size() != configuredParameters.size()) {
            throw new BusinessException("提交的检测参数数量与套餐配置不一致");
        }
        for (DetectionParameter parameter : configuredParameters) {
            DetectionItemCommand item = itemMap.get(parameter.getId());
            if (item == null) {
                throw new BusinessException("缺少检测参数结果：" + parameter.getParameterName());
            }
            validateItemAgainstConfig(item, parameter);
        }
        return itemMap;
    }

    private Map<Long, DetectionItemCommand> validatePendingSubmittedItems(List<DetectionItemCommand> items,
                                                                          List<DetectionParameter> configuredParameters,
                                                                          Map<Long, DetectionItem> pendingItemMap,
                                                                          CurrentUser currentUser,
                                                                          Long expectedItemId) {
        if (pendingItemMap.isEmpty()) {
            throw new BusinessException("当前样品尚未生成参数子流程，请重新登录样品后再提交");
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessException("检测结果明细不能为空");
        }
        if (expectedItemId != null && items.size() != 1) {
            throw new BusinessException("单个子流程提交时只能提交一条检测参数结果");
        }

        // 单个子流程录入时，必须命中当前指定子流程，且只能由被分配的检测员本人提交。
        Map<Long, DetectionParameter> parameterMap = configuredParameters.stream()
                .collect(Collectors.toMap(DetectionParameter::getId, parameter -> parameter, (left, right) -> left));
        Map<Long, DetectionItemCommand> itemMap = new LinkedHashMap<>();
        boolean admin = hasFullAccessRole(currentUser);

        for (DetectionItemCommand item : items) {
            if (itemMap.put(item.getParameterId(), item) != null) {
                throw new BusinessException("检测结果明细中存在重复的检测参数");
            }
            DetectionParameter parameter = parameterMap.get(item.getParameterId());
            if (parameter == null) {
                throw new BusinessException("提交结果中存在未配置的检测参数");
            }
            validateItemAgainstConfig(item, parameter);

            DetectionItem pendingItem = pendingItemMap.get(item.getParameterId());
            if (pendingItem == null) {
                throw new BusinessException("检测参数“" + parameter.getParameterName() + "”未生成对应的子流程");
            }
            if (expectedItemId != null && !expectedItemId.equals(pendingItem.getId())) {
                throw new BusinessException("当前提交的不是指定子流程，请刷新页面后重试");
            }
            if (pendingItem.getDetectorId() == null) {
                throw new BusinessException("检测参数“" + parameter.getParameterName() + "”尚未分配检测员");
            }
            if (LabWorkflowConstants.DetectionStatus.ENTERED.equals(pendingItem.getItemStatus())
                    || LabWorkflowConstants.DetectionStatus.SUBMITTED.equals(pendingItem.getItemStatus())
                    || LabWorkflowConstants.DetectionStatus.APPROVED.equals(pendingItem.getItemStatus())) {
                throw new BusinessException("检测参数“" + parameter.getParameterName() + "”已提交结果，不能重复录入");
            }
            if (!admin && !pendingItem.getDetectorId().equals(currentUser.getUserId())) {
                throw new BusinessException("当前登录人不是该子流程的检测员，不能提交检测结果");
            }
        }
        return itemMap;
    }

    private void validateItemAgainstConfig(DetectionItemCommand item, DetectionParameter parameter) {
        if (!StrUtil.equals(item.getParameterName(), parameter.getParameterName())) {
            throw new BusinessException("检测参数名称与配置不一致：" + parameter.getParameterName());
        }
        if (StrUtil.isNotBlank(item.getUnit())
                && !StrUtil.equals(item.getUnit(), StrUtil.blankToDefault(parameter.getUnit(), ""))) {
            throw new BusinessException("检测参数单位与配置不一致：" + parameter.getParameterName());
        }
        // 文本选项模式：跳过标准上下限比对，改为比对选项值快照
        if (StrUtil.isNotBlank(parameter.getOptionValues())) {
            if (!StrUtil.equals(StrUtil.blankToDefault(item.getOptionValues(), ""),
                    StrUtil.blankToDefault(parameter.getOptionValues(), ""))) {
                throw new BusinessException("检测参数选项值与配置不一致：" + parameter.getParameterName());
            }
            validateResultWithinStandardRange(parameter, item.getResultValue());
            return;
        }
        if (item.getStandardMin() != null && compareNullableDecimal(item.getStandardMin(), parameter.getStandardMin()) != 0) {
            throw new BusinessException("检测参数标准下限与配置不一致：" + parameter.getParameterName());
        }
        if (item.getStandardMax() != null && compareNullableDecimal(item.getStandardMax(), parameter.getStandardMax()) != 0) {
            throw new BusinessException("检测参数标准上限与配置不一致：" + parameter.getParameterName());
        }
        validateResultWithinStandardRange(parameter, item.getResultValue());
    }

    private boolean hasFullAccessRole(CurrentUser currentUser) {
        return currentUser != null
                && ("ADMIN".equalsIgnoreCase(currentUser.getRoleCode())
                || "DIRECTOR".equalsIgnoreCase(currentUser.getRoleCode()));
    }

    private void validateResultWithinStandardRange(DetectionParameter parameter, BigDecimal resultValue) {
        if (parameter == null || resultValue == null) {
            return;
        }
        if (isExceeded(parameter, resultValue)) {
            throw new BusinessException("检测结果存在异常，禁止录入：" + parameter.getParameterName()
                    + "，标准范围=" + formatStandardRange(parameter)
                    + "，检测结果=" + resultValue);
        }
    }

    private String formatStandardRange(DetectionParameter parameter) {
        if (parameter == null) {
            return "-";
        }
        if (StrUtil.isNotBlank(parameter.getOptionValues())) {
            List<String> options = parseOptionValues(parameter.getOptionValues());
            return options.isEmpty() ? "-" : String.join(" / ", options);
        }
        BigDecimal min = parameter.getStandardMin();
        BigDecimal max = parameter.getStandardMax();
        String unit = StrUtil.blankToDefault(parameter.getUnit(), "");
        if (min != null && max != null) {
            return min + " - " + max + unit;
        }
        if (min != null) {
            return ">=" + min + unit;
        }
        if (max != null) {
            return "<=" + max + unit;
        }
        return "-";
    }

    private void applyRecordDetectorSummary(DetectionRecord record, List<DetectionItem> items, CurrentUser currentUser) {
        Set<Long> detectorIds = items.stream()
                .map(DetectionItem::getDetectorId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> detectorNames = items.stream()
                .map(DetectionItem::getDetectorName)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (detectorIds.size() == 1 && detectorNames.size() == 1) {
            record.setDetectorId(detectorIds.iterator().next());
            record.setDetectorName(detectorNames.iterator().next());
            return;
        }
        if (detectorIds.size() > 1) {
            record.setDetectorId(null);
            record.setDetectorName("协同检测");
            return;
        }
        record.setDetectorId(currentUser.getUserId());
        record.setDetectorName(currentUser.getRealName());
    }

    private void applyPendingRecordStatus(DetectionRecord record,
                                          LabSample sample,
                                          List<DetectionItem> pendingItems,
                                          CurrentUser currentUser) {
        boolean allApproved = !pendingItems.isEmpty() && pendingItems.stream()
                .allMatch(item -> LabWorkflowConstants.DetectionStatus.APPROVED.equals(item.getItemStatus()));
        if (allApproved) {
            record.setDetectionResult(buildResultFromPendingItems(pendingItems));
            record.setDetectionStatus(LabWorkflowConstants.DetectionStatus.APPROVED);
            labSampleService.updateStatus(
                    sample.getId(),
                    LabWorkflowConstants.SampleStatus.COMPLETED,
                    record.getDetectionResult(),
                    "检测流程已完成：样品编号=" + sample.getSampleNo()
                            + "，检测套餐=" + record.getDetectionTypeName()
                            + "，检测员=" + StrUtil.blankToDefault(record.getDetectorName(), currentUser.getRealName())
                            + "，结果=" + LabWorkflowConstants.getDetectionResultLabel(record.getDetectionResult()));
            return;
        }

        boolean allReadyForReview = !pendingItems.isEmpty() && pendingItems.stream()
                .allMatch(item -> LabWorkflowConstants.DetectionStatus.ENTERED.equals(item.getItemStatus())
                        || LabWorkflowConstants.DetectionStatus.SUBMITTED.equals(item.getItemStatus())
                        || LabWorkflowConstants.DetectionStatus.APPROVED.equals(item.getItemStatus()));
        if (allReadyForReview) {
            for (DetectionItem item : pendingItems) {
                if (LabWorkflowConstants.DetectionStatus.ENTERED.equals(item.getItemStatus())) {
                    item.setItemStatus(LabWorkflowConstants.DetectionStatus.SUBMITTED);
                    detectionItemMapper.updateById(item);
                }
            }
            record.setDetectionResult(buildResultFromPendingItems(pendingItems));
            record.setDetectionStatus(LabWorkflowConstants.DetectionStatus.SUBMITTED);
            labSampleService.updateStatus(
                    sample.getId(),
                    LabWorkflowConstants.SampleStatus.REVIEWING,
                    record.getDetectionResult(),
                    "检测结果已提交待审核：样品编号=" + sample.getSampleNo()
                            + "，检测套餐=" + record.getDetectionTypeName()
                            + "，检测员=" + StrUtil.blankToDefault(record.getDetectorName(), currentUser.getRealName())
                            + "，结果=" + LabWorkflowConstants.getDetectionResultLabel(record.getDetectionResult()));
            return;
        }

        record.setDetectionResult(null);
        boolean hasRejected = pendingItems.stream()
                .anyMatch(item -> LabWorkflowConstants.DetectionStatus.REJECTED.equals(item.getItemStatus()));
        if (hasRejected) {
            record.setDetectionStatus(LabWorkflowConstants.DetectionStatus.WAIT_DETECT);
            return;
        }
        boolean hasUnassigned = pendingItems.stream().anyMatch(item -> item.getDetectorId() == null
                || LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN.equals(item.getItemStatus()));
        record.setDetectionStatus(hasUnassigned
                ? LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN
                : LabWorkflowConstants.DetectionStatus.WAIT_DETECT);
    }

    private int compareNullableDecimal(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private List<Long> parseParameterIds(String parameterIdsText) {
        List<Long> parameterIds = new ArrayList<>();
        if (StrUtil.isBlank(parameterIdsText)) {
            return parameterIds;
        }
        for (String rawId : StrUtil.split(parameterIdsText, ',')) {
            String idText = StrUtil.trim(rawId);
            if (StrUtil.isBlank(idText)) {
                continue;
            }
            try {
                parameterIds.add(Long.valueOf(idText));
            } catch (NumberFormatException ex) {
                throw new BusinessException("检测套餐参数ID格式错误，无法解析");
            }
        }
        return parameterIds;
    }

    private String buildResult(List<DetectionParameter> configuredParameters, Map<Long, DetectionItemCommand> itemMap) {
        return configuredParameters.stream()
                .anyMatch(parameter -> isExceeded(parameter, itemMap.get(parameter.getId()).getResultValue()))
                ? LabWorkflowConstants.DetectionResult.ABNORMAL
                : LabWorkflowConstants.DetectionResult.NORMAL;
    }

    private String buildResultFromPendingItems(List<DetectionItem> items) {
        return items.stream().anyMatch(item -> Integer.valueOf(1).equals(item.getExceedFlag()))
                ? LabWorkflowConstants.DetectionResult.ABNORMAL
                : LabWorkflowConstants.DetectionResult.NORMAL;
    }

    private void fillMethodBasis(List<DetectionItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Long> methodIds = items.stream()
                .map(DetectionItem::getMethodId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (methodIds.isEmpty()) {
            return;
        }
        Map<Long, DetectionMethod> methodMap = detectionMethodMapper.selectList(new LambdaQueryWrapper<DetectionMethod>()
                        .in(DetectionMethod::getId, methodIds))
                .stream()
                .collect(Collectors.toMap(DetectionMethod::getId, method -> method, (left, right) -> left));
        for (DetectionItem item : items) {
            if (item == null || item.getMethodId() == null) {
                continue;
            }
            DetectionMethod method = methodMap.get(item.getMethodId());
            if (method != null) {
                item.setMethodBasis(method.getMethodBasis());
            }
        }
    }

    private boolean isExceeded(DetectionParameter parameter, BigDecimal resultValue) {
        if (resultValue == null) {
            return false;
        }
        // 文本选项模式：只需校验索引在有效范围内
        if (StrUtil.isNotBlank(parameter.getOptionValues())) {
            List<String> options = parseOptionValues(parameter.getOptionValues());
            if (options.isEmpty()) {
                return false;
            }
            int index = resultValue.intValue();
            return index < 0 || index >= options.size();
        }
        if (parameter.getStandardMin() != null && resultValue.compareTo(parameter.getStandardMin()) < 0) {
            return true;
        }
        return parameter.getStandardMax() != null && resultValue.compareTo(parameter.getStandardMax()) > 0;
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("当前登录用户信息失效，请重新登录");
        }
        return currentUser;
    }

    private List<String> parseOptionValues(String optionValuesJson) {
        if (StrUtil.isBlank(optionValuesJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(optionValuesJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
