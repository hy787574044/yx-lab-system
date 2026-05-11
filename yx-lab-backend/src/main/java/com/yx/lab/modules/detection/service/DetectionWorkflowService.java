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
import com.yx.lab.modules.detection.entity.DetectionParameter;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.entity.DetectionStep;
import com.yx.lab.modules.detection.entity.DetectionType;
import com.yx.lab.modules.detection.mapper.DetectionItemMapper;
import com.yx.lab.modules.detection.mapper.DetectionParameterMapper;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.detection.mapper.DetectionStepMapper;
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

    private final DetectionTypeMapper detectionTypeMapper;

    private final DetectionParameterMapper detectionParameterMapper;

    private final DetectionStepMapper detectionStepMapper;

    private final LabSampleMapper labSampleMapper;

    private final ReviewRecordMapper reviewRecordMapper;

    private final LabSampleService labSampleService;

    private final DetectionPendingFlowService detectionPendingFlowService;

    private final ObjectMapper objectMapper;

    /**
     * 分页查询检测主流程。
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
     * 查询检测主流程详情及其参数子流程。
     */
    public DetectionRecordDetailVO detail(Long id) {
        DetectionRecord record = detectionRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("检测主流程不存在");
        }
        List<DetectionItem> items = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getRecordId, id)
                .orderByAsc(DetectionItem::getCreatedTime));
        DetectionRecordDetailVO vo = new DetectionRecordDetailVO();
        vo.setRecord(record);
        vo.setItems(items);
        return vo;
    }

    /**
     * 保存参数子流程的检测员分配。
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignDetectors(Long recordId, DetectionAssignCommand command) {
        detectionPendingFlowService.assignDetectors(recordId, command);
    }

    /**
     * 提交检测结果。
     */
    @Transactional(rollbackFor = Exception.class)
    public void submit(DetectionSubmitCommand command) {
        LabSample sample = labSampleMapper.selectById(command.getSampleId());
        if (sample == null) {
            throw new BusinessException("样品不存在");
        }
        if (!LabWorkflowConstants.canSubmitDetection(sample.getSampleStatus())) {
            throw new BusinessException("当前样品状态不允许提交检测");
        }
        if (LabWorkflowConstants.SampleStatus.RETEST.equals(sample.getSampleStatus())) {
            validateRetestSubmission(sample);
        }

        DetectionType detectionType = detectionTypeMapper.selectById(command.getDetectionTypeId());
        DetectionType usableType = requireUsableType(command, detectionType);
        List<DetectionParameter> configuredParameters = resolveConfiguredParameters(usableType, sample);
        ensureTypeHasSteps(usableType.getId());
        Map<Long, DetectionItemCommand> itemMap = validateSubmittedItems(command.getItems(), configuredParameters);

        CurrentUser currentUser = requireCurrentUser();
        DetectionRecord activeRecord = detectionPendingFlowService.createPendingFlowIfMissing(sample);
        if (activeRecord == null) {
            activeRecord = detectionPendingFlowService.findActiveRecordBySampleId(sample.getId());
        }

        if (activeRecord != null && LabWorkflowConstants.canAssignDetection(activeRecord.getDetectionStatus())) {
            submitPendingRecord(activeRecord, sample, usableType, configuredParameters, itemMap, currentUser, command.getAbnormalRemark());
            return;
        }

        Long pendingCount = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getSampleId, sample.getId())
                .eq(DetectionRecord::getDetectionStatus, LabWorkflowConstants.DetectionStatus.SUBMITTED));
        if (pendingCount != null && pendingCount > 0) {
            throw new BusinessException("当前样品已存在待审查的检测记录");
        }
        validateDetectorBinding(usableType, currentUser);
        insertSubmittedRecord(sample, usableType, configuredParameters, itemMap, currentUser, command.getAbnormalRemark());
    }

    private void submitPendingRecord(DetectionRecord record,
                                     LabSample sample,
                                     DetectionType detectionType,
                                     List<DetectionParameter> configuredParameters,
                                     Map<Long, DetectionItemCommand> itemMap,
                                     CurrentUser currentUser,
                                     String abnormalRemark) {
        List<DetectionItem> pendingItems = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getRecordId, record.getId())
                .orderByAsc(DetectionItem::getCreatedTime));
        Map<Long, DetectionItem> pendingItemMap = pendingItems.stream()
                .collect(Collectors.toMap(DetectionItem::getParameterId, item -> item, (left, right) -> left));
        validatePendingAssignmentsForSubmit(configuredParameters, pendingItemMap, currentUser);

        for (DetectionParameter parameter : configuredParameters) {
            DetectionItem pendingItem = pendingItemMap.get(parameter.getId());
            DetectionItemCommand itemCommand = itemMap.get(parameter.getId());
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
        record.setAbnormalRemark(StrUtil.trim(abnormalRemark));
        record.setDetectionResult(buildResult(configuredParameters, itemMap));
        record.setDetectionStatus(LabWorkflowConstants.DetectionStatus.SUBMITTED);
        applyRecordDetectorSummary(record, pendingItems, currentUser);
        detectionRecordMapper.updateById(record);

        labSampleService.updateStatus(
                sample.getId(),
                LabWorkflowConstants.SampleStatus.REVIEWING,
                record.getDetectionResult(),
                "检测提交：封签号=" + sample.getSealNo()
                        + "，检测套餐=" + record.getDetectionTypeName()
                        + "，检测人=" + StrUtil.blankToDefault(record.getDetectorName(), currentUser.getRealName())
                        + "，结果=" + record.getDetectionResult());
    }

    private void insertSubmittedRecord(LabSample sample,
                                       DetectionType detectionType,
                                       List<DetectionParameter> configuredParameters,
                                       Map<Long, DetectionItemCommand> itemMap,
                                       CurrentUser currentUser,
                                       String abnormalRemark) {
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
                "检测提交：封签号=" + sample.getSealNo()
                        + "，检测套餐=" + record.getDetectionTypeName()
                        + "，检测人=" + currentUser.getRealName()
                        + "，结果=" + record.getDetectionResult());
    }

    private void validateRetestSubmission(LabSample sample) {
        ReviewRecord latestReview = reviewRecordMapper.selectOne(new LambdaQueryWrapper<ReviewRecord>()
                .eq(ReviewRecord::getSampleId, sample.getId())
                .orderByDesc(ReviewRecord::getReviewTime)
                .orderByDesc(ReviewRecord::getCreatedTime)
                .last("limit 1"));
        if (latestReview == null) {
            throw new BusinessException("当前样品没有审查驳回记录，不能按重检流程提交");
        }
        if (!LabWorkflowConstants.ReviewResult.REJECTED.equals(latestReview.getReviewResult())) {
            throw new BusinessException("当前样品最近一次审查结果不是驳回，不能进入重检流程");
        }
    }

    private DetectionType requireUsableType(DetectionSubmitCommand command, DetectionType detectionType) {
        if (detectionType == null) {
            throw new BusinessException("检测套餐不存在");
        }
        if (!Integer.valueOf(1).equals(detectionType.getEnabled())) {
            throw new BusinessException("当前检测套餐已停用，不能提交检测");
        }
        if (StrUtil.isNotBlank(command.getDetectionTypeName())
                && !StrUtil.equals(command.getDetectionTypeName(), detectionType.getTypeName())) {
            throw new BusinessException("检测套餐名称与后端配置不一致，请刷新后重试");
        }
        return detectionType;
    }

    private void validateDetectorBinding(DetectionType detectionType, CurrentUser currentUser) {
        if (detectionType.getDetectorId() == null) {
            return;
        }
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("当前登录信息已失效，请重新登录");
        }
        if ("ADMIN".equalsIgnoreCase(currentUser.getRoleCode())) {
            return;
        }
        if (!detectionType.getDetectorId().equals(currentUser.getUserId())) {
            throw new BusinessException("当前检测套餐已绑定检测员“" + detectionType.getDetectorName() + "”，不能由当前用户提交");
        }
    }

    private List<DetectionParameter> resolveConfiguredParameters(DetectionType detectionType, LabSample sample) {
        List<Long> parameterIds = resolveSampleParameterIds(sample, detectionType);
        if (parameterIds.isEmpty()) {
            throw new BusinessException("当前检测套餐未配置检测参数，不能提交检测");
        }
        List<DetectionParameter> parameters = detectionParameterMapper.selectList(new LambdaQueryWrapper<DetectionParameter>()
                .in(DetectionParameter::getId, parameterIds));
        Map<Long, DetectionParameter> parameterMap = parameters.stream()
                .collect(Collectors.toMap(DetectionParameter::getId, parameter -> parameter));
        List<DetectionParameter> orderedParameters = new ArrayList<>();
        for (Long parameterId : parameterIds) {
            DetectionParameter parameter = parameterMap.get(parameterId);
            if (parameter == null) {
                throw new BusinessException("检测套餐绑定的检测参数不存在，请先修正检测配置");
            }
            if (!Integer.valueOf(1).equals(parameter.getEnabled())) {
                throw new BusinessException("检测套餐绑定了已停用的检测参数，请先修正检测配置");
            }
            orderedParameters.add(parameter);
        }
        return orderedParameters;
    }

    private void ensureTypeHasSteps(Long typeId) {
        Long stepCount = detectionStepMapper.selectCount(new LambdaQueryWrapper<DetectionStep>()
                .eq(DetectionStep::getTypeId, typeId));
        if (stepCount == null || stepCount == 0) {
            throw new BusinessException("当前检测套餐未配置检测步骤，不能提交检测");
        }
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
            throw new BusinessException("样品检测套餐快照格式不正确，请重新登录样品");
        }
    }

    private Map<Long, DetectionItemCommand> validateSubmittedItems(List<DetectionItemCommand> items,
                                                                   List<DetectionParameter> configuredParameters) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("检测结果不能为空");
        }
        Map<Long, DetectionItemCommand> itemMap = new LinkedHashMap<>();
        for (DetectionItemCommand item : items) {
            if (itemMap.put(item.getParameterId(), item) != null) {
                throw new BusinessException("同一检测参数不能重复提交");
            }
        }
        if (itemMap.size() != configuredParameters.size()) {
            throw new BusinessException("提交的检测参数数量与套餐配置不一致");
        }
        for (DetectionParameter parameter : configuredParameters) {
            DetectionItemCommand item = itemMap.get(parameter.getId());
            if (item == null) {
                throw new BusinessException("缺少检测参数：" + parameter.getParameterName());
            }
            validateItemAgainstConfig(item, parameter);
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

    private void validatePendingAssignmentsForSubmit(List<DetectionParameter> configuredParameters,
                                                     Map<Long, DetectionItem> pendingItemMap,
                                                     CurrentUser currentUser) {
        if (pendingItemMap.isEmpty()) {
            throw new BusinessException("当前样品还没有生成参数子流程，不能直接提交检测");
        }
        boolean admin = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRoleCode());
        Set<Long> ownerIds = new LinkedHashSet<>();
        for (DetectionParameter parameter : configuredParameters) {
            DetectionItem pendingItem = pendingItemMap.get(parameter.getId());
            if (pendingItem == null) {
                throw new BusinessException("检测参数“" + parameter.getParameterName() + "”缺少待分配子流程");
            }
            if (pendingItem.getDetectorId() == null) {
                throw new BusinessException("检测参数“" + parameter.getParameterName() + "”尚未分配检测员");
            }
            ownerIds.add(pendingItem.getDetectorId());
            if (!admin && !pendingItem.getDetectorId().equals(currentUser.getUserId())) {
                throw new BusinessException("当前用户只能提交分配给自己的检测参数，请先按人员分工处理");
            }
        }
        if (!admin && ownerIds.size() > 1) {
            throw new BusinessException("当前样品存在多名检测员分工，普通检测员不能一次性代替他人提交全部结果");
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
            record.setDetectorName("多人协同");
            return;
        }
        record.setDetectorId(currentUser.getUserId());
        record.setDetectorName(currentUser.getRealName());
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
                throw new BusinessException("检测套餐参数配置格式不合法，请先修正检测配置");
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
            throw new BusinessException("当前登录信息已失效，请重新登录");
        }
        return currentUser;
    }
}
