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
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.detection.dto.DetectionAssignCommand;
import com.yx.lab.modules.detection.dto.DetectionItemCommand;
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

    /**
     * 分页查询检测主流程列表。
     *
     * @param query 查询条件
     * @return 检测主流程分页结果
     */
    public PageResult<DetectionRecord> page(DetectionRecordQuery query) {
        detectionPendingFlowService.syncPendingFlowsForOpenSamples();
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        Page<DetectionRecord> page = detectionRecordMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<DetectionRecord>()
                        .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                                .like(DetectionRecord::getSampleNo, query.getKeyword())
                                .or()
                                .like(DetectionRecord::getSealNo, query.getKeyword())
                                .or()
                                .like(DetectionRecord::getDetectionTypeName, query.getKeyword()))
                        .eq(StrUtil.isNotBlank(query.getDetectionStatus()), DetectionRecord::getDetectionStatus, query.getDetectionStatus())
                        .eq(Boolean.TRUE.equals(query.getMine()) && currentUser != null,
                                DetectionRecord::getDetectorId,
                                currentUser == null ? null : currentUser.getUserId())
                        .orderByDesc(DetectionRecord::getDetectionTime)
                        .orderByDesc(DetectionRecord::getCreatedTime));
        detectionPendingFlowService.fillRecordSummaries(page.getRecords());
        return new PageResult<>(page.getTotal(), page.getRecords());
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
                    command.getItemId());
            return;
        }

        Map<Long, DetectionItemCommand> itemMap = validateSubmittedItems(command.getItems(), configuredParameters);
        Long pendingCount = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getSampleId, sample.getId())
                .eq(DetectionRecord::getDetectionStatus, LabWorkflowConstants.DetectionStatus.SUBMITTED));
        if (pendingCount != null && pendingCount > 0) {
            throw new BusinessException("当前样品已存在已提交的检测记录，请勿重复提交");
        }
        validateDetectorBinding(usableType, currentUser);
        insertSubmittedRecord(sample, usableType, configuredParameters, itemMap, currentUser, command.getAbnormalRemark());
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
        // 主流程的检测员、状态和结果，统一根据全部子流程的最新状态重新汇总。
        applyRecordDetectorSummary(record, pendingItems, currentUser);
        applyPendingRecordStatus(record, sample, pendingItems, currentUser);
        detectionRecordMapper.updateById(record);
    }

    private void insertSubmittedRecord(LabSample sample,
                                       DetectionType detectionType,
                                       List<DetectionParameter> configuredParameters,
                                       Map<Long, DetectionItemCommand> itemMap,
                                       CurrentUser currentUser,
                                       String abnormalRemark) {
        // 兼容旧的整单提交模式：直接生成一条主流程和完整的参数结果明细。
        DetectionRecord record = new DetectionRecord();
        record.setSampleId(sample.getId());
        record.setSampleNo(sample.getSampleNo());
        record.setSealNo(sample.getSealNo());
        record.setDetectionTypeId(detectionType.getId());
        record.setDetectionTypeName(detectionType.getTypeName());
        record.setDetectionTime(LocalDateTime.now());
        record.setDetectorId(currentUser.getUserId());
        record.setDetectorName(currentUser.getRealName());
        record.setAbnormalRemark(StrUtil.trim(abnormalRemark));
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
                "检测结果已提交：封签号=" + sample.getSealNo()
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
        if ("ADMIN".equalsIgnoreCase(currentUser.getRoleCode())) {
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
        boolean admin = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRoleCode());

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
            if (LabWorkflowConstants.DetectionStatus.SUBMITTED.equals(pendingItem.getItemStatus())
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
        if (item.getStandardMin() != null && compareNullableDecimal(item.getStandardMin(), parameter.getStandardMin()) != 0) {
            throw new BusinessException("检测参数标准下限与配置不一致：" + parameter.getParameterName());
        }
        if (item.getStandardMax() != null && compareNullableDecimal(item.getStandardMax(), parameter.getStandardMax()) != 0) {
            throw new BusinessException("检测参数标准上限与配置不一致：" + parameter.getParameterName());
        }
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
                    "检测流程已完成：封签号=" + sample.getSealNo()
                            + "，检测套餐=" + record.getDetectionTypeName()
                            + "，检测员=" + StrUtil.blankToDefault(record.getDetectorName(), currentUser.getRealName())
                            + "，结果=" + LabWorkflowConstants.getDetectionResultLabel(record.getDetectionResult()));
            return;
        }

        boolean allReadyForReview = !pendingItems.isEmpty() && pendingItems.stream()
                .allMatch(item -> LabWorkflowConstants.DetectionStatus.SUBMITTED.equals(item.getItemStatus())
                        || LabWorkflowConstants.DetectionStatus.APPROVED.equals(item.getItemStatus()));
        if (allReadyForReview) {
            record.setDetectionResult(buildResultFromPendingItems(pendingItems));
            record.setDetectionStatus(LabWorkflowConstants.DetectionStatus.SUBMITTED);
            labSampleService.updateStatus(
                    sample.getId(),
                    LabWorkflowConstants.SampleStatus.REVIEWING,
                    record.getDetectionResult(),
                    "检测结果已提交待审核：封签号=" + sample.getSealNo()
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
}
