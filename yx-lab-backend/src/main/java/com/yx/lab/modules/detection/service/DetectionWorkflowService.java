package com.yx.lab.modules.detection.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.util.PageUtils;
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
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.sample.service.LabSampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public PageResult<DetectionRecord> page(DetectionRecordQuery query) {
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
                        .eq(Boolean.TRUE.equals(query.getMine()), DetectionRecord::getDetectorId, currentUser.getUserId())
                        .orderByDesc(DetectionRecord::getDetectionTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public DetectionRecordDetailVO detail(Long id) {
        DetectionRecord record = detectionRecordMapper.selectById(id);
        List<DetectionItem> items = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getRecordId, id)
                .orderByAsc(DetectionItem::getCreatedTime));
        DetectionRecordDetailVO vo = new DetectionRecordDetailVO();
        vo.setRecord(record);
        vo.setItems(items);
        return vo;
    }

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

        Long pendingCount = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getSampleId, sample.getId())
                .eq(DetectionRecord::getDetectionStatus, LabWorkflowConstants.DetectionStatus.SUBMITTED));
        if (pendingCount != null && pendingCount > 0) {
            throw new BusinessException("当前样品已存在待审核的检测记录");
        }

        CurrentUser currentUser = SecurityContext.getCurrentUser();
        DetectionType detectionType = detectionTypeMapper.selectById(command.getDetectionTypeId());
        DetectionType usableType = requireUsableType(command, detectionType);
        validateDetectorBinding(usableType, currentUser);
        List<DetectionParameter> configuredParameters = resolveConfiguredParameters(usableType);
        ensureTypeHasSteps(usableType.getId());
        Map<Long, DetectionItemCommand> itemMap = validateSubmittedItems(command.getItems(), configuredParameters);

        DetectionRecord record = new DetectionRecord();
        record.setSampleId(sample.getId());
        record.setSampleNo(sample.getSampleNo());
        record.setSealNo(sample.getSealNo());
        record.setDetectionTypeId(command.getDetectionTypeId());
        record.setDetectionTypeName(usableType.getTypeName());
        record.setDetectionTime(LocalDateTime.now());
        record.setDetectorId(currentUser.getUserId());
        record.setDetectorName(currentUser.getRealName());
        record.setAbnormalRemark(command.getAbnormalRemark());
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
            item.setExceedFlag(isExceeded(parameter, itemCommand.getResultValue()) ? 1 : 0);
            detectionItemMapper.insert(item);
        }

        labSampleService.updateStatus(
                sample.getId(),
                LabWorkflowConstants.SampleStatus.REVIEWING,
                record.getDetectionResult(),
                "检测提交：封签号=" + sample.getSealNo()
                        + "，检测类型=" + record.getDetectionTypeName()
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
            throw new BusinessException("当前样品没有审核驳回记录，不能按重检流程提交");
        }
        if (!LabWorkflowConstants.ReviewResult.REJECTED.equals(latestReview.getReviewResult())) {
            throw new BusinessException("当前样品最近一次审核结果不是驳回，不能进入重检流程");
        }
    }

    private DetectionType requireUsableType(DetectionSubmitCommand command, DetectionType detectionType) {
        if (detectionType == null) {
            throw new BusinessException("检测类型不存在");
        }
        if (!Integer.valueOf(1).equals(detectionType.getEnabled())) {
            throw new BusinessException("当前检测类型已停用，不能提交检测");
        }
        if (StrUtil.isNotBlank(command.getDetectionTypeName())
                && !StrUtil.equals(command.getDetectionTypeName(), detectionType.getTypeName())) {
            throw new BusinessException("检测类型名称与配置不一致，请刷新后重试");
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
            throw new BusinessException("当前检测项目已绑定检测员“" + detectionType.getDetectorName() + "”，不能由当前用户提交");
        }
    }

    private List<DetectionParameter> resolveConfiguredParameters(DetectionType detectionType) {
        List<Long> parameterIds = parseParameterIds(detectionType.getParameterIds());
        if (parameterIds.isEmpty()) {
            throw new BusinessException("当前检测类型未配置检测参数，不能提交检测");
        }
        List<DetectionParameter> parameters = detectionParameterMapper.selectList(new LambdaQueryWrapper<DetectionParameter>()
                .in(DetectionParameter::getId, parameterIds));
        Map<Long, DetectionParameter> parameterMap = parameters.stream()
                .collect(Collectors.toMap(DetectionParameter::getId, parameter -> parameter));
        List<DetectionParameter> orderedParameters = new ArrayList<>();
        for (Long parameterId : parameterIds) {
            DetectionParameter parameter = parameterMap.get(parameterId);
            if (parameter == null) {
                throw new BusinessException("检测类型绑定的参数不存在，请先修正检测配置");
            }
            if (!Integer.valueOf(1).equals(parameter.getEnabled())) {
                throw new BusinessException("检测类型绑定了已停用的检测参数，请先修正检测配置");
            }
            orderedParameters.add(parameter);
        }
        return orderedParameters;
    }

    private void ensureTypeHasSteps(Long typeId) {
        Long stepCount = detectionStepMapper.selectCount(new LambdaQueryWrapper<DetectionStep>()
                .eq(DetectionStep::getTypeId, typeId));
        if (stepCount == null || stepCount == 0) {
            throw new BusinessException("当前检测类型未配置检测步骤，不能提交检测");
        }
    }

    private Map<Long, DetectionItemCommand> validateSubmittedItems(List<DetectionItemCommand> items,
                                                                   List<DetectionParameter> configuredParameters) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("检测项不能为空");
        }
        Map<Long, DetectionItemCommand> itemMap = new LinkedHashMap<>();
        for (DetectionItemCommand item : items) {
            if (itemMap.put(item.getParameterId(), item) != null) {
                throw new BusinessException("同一检测参数不能重复提交");
            }
        }
        if (itemMap.size() != configuredParameters.size()) {
            throw new BusinessException("提交的检测项数量与检测配置不一致");
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
        if (StrUtil.isNotBlank(item.getUnit()) && !StrUtil.equals(item.getUnit(), StrUtil.blankToDefault(parameter.getUnit(), ""))) {
            throw new BusinessException("检测参数单位与配置不一致：" + parameter.getParameterName());
        }
        if (item.getStandardMin() != null && compareNullableDecimal(item.getStandardMin(), parameter.getStandardMin()) != 0) {
            throw new BusinessException("检测参数标准下限与配置不一致：" + parameter.getParameterName());
        }
        if (item.getStandardMax() != null && compareNullableDecimal(item.getStandardMax(), parameter.getStandardMax()) != 0) {
            throw new BusinessException("检测参数标准上限与配置不一致：" + parameter.getParameterName());
        }
    }

    private int compareNullableDecimal(java.math.BigDecimal left, java.math.BigDecimal right) {
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
                throw new BusinessException("检测类型参数配置格式不合法，请先修正检测配置");
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

    private boolean isExceeded(DetectionParameter parameter, java.math.BigDecimal resultValue) {
        if (resultValue == null) {
            return false;
        }
        if (parameter.getStandardMin() != null
                && resultValue.compareTo(parameter.getStandardMin()) < 0) {
            return true;
        }
        return parameter.getStandardMax() != null
                && resultValue.compareTo(parameter.getStandardMax()) > 0;
    }
}
