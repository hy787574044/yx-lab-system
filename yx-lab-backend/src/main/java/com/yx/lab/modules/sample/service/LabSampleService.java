package com.yx.lab.modules.sample.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.detection.entity.DetectionMethod;
import com.yx.lab.modules.detection.entity.DetectionType;
import com.yx.lab.modules.detection.mapper.DetectionMethodMapper;
import com.yx.lab.modules.detection.mapper.DetectionTypeMapper;
import com.yx.lab.modules.detection.service.DetectionPendingFlowService;
import com.yx.lab.modules.sample.dto.LabSampleQuery;
import com.yx.lab.modules.sample.dto.SampleDetectionConfigItem;
import com.yx.lab.modules.sample.dto.SampleLoginCommand;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.sample.mapper.SamplingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabSampleService {

    private final LabSampleMapper labSampleMapper;

    private final SamplingTaskMapper samplingTaskMapper;

    private final DetectionTypeMapper detectionTypeMapper;

    private final DetectionMethodMapper detectionMethodMapper;

    private final DetectionPendingFlowService detectionPendingFlowService;

    private final ObjectMapper objectMapper;

    public PageResult<LabSample> page(LabSampleQuery query) {
        Page<LabSample> page = labSampleMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<LabSample>()
                        .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                                .like(LabSample::getSampleNo, query.getKeyword())
                                .or()
                                .like(LabSample::getSealNo, query.getKeyword())
                                .or()
                                .like(LabSample::getPointName, query.getKeyword()))
                        .eq(StrUtil.isNotBlank(query.getSampleStatus()), LabSample::getSampleStatus, query.getSampleStatus())
                        .eq(StrUtil.isNotBlank(query.getSampleType()), LabSample::getSampleType, query.getSampleType())
                        .orderByDesc(LabSample::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public LabSample detail(Long id) {
        return labSampleMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public LabSample loginSample(SampleLoginCommand command) {
        CurrentUser currentUser = requireCurrentUser();
        String commandSealNo = normalizeSealNo(command.getSealNo());
        SamplingTask task = resolveTaskForLogin(command);
        if (task != null) {
            validateTaskForSampleLogin(task, currentUser, commandSealNo);
        } else {
            validateSamplerOperator(command, currentUser);
        }
        DetectionType detectionType = resolveDetectionType(command);
        List<SampleDetectionConfigItem> detectionConfigItems = normalizeDetectionConfigItems(command, detectionType);

        String sealNo = resolveSealNo(commandSealNo, task);
        validateSealNoUniqueness(sealNo);

        LabSample sample = new LabSample();
        sample.setSampleNo(generateSampleNo());
        sample.setSealNo(sealNo);
        sample.setTaskId(task == null ? command.getTaskId() : task.getId());
        sample.setPointId(command.getPointId() != null ? command.getPointId() : (task == null ? null : task.getPointId()));
        sample.setPointName(StrUtil.isNotBlank(command.getPointName()) ? command.getPointName() : (task == null ? null : task.getPointName()));
        sample.setSampleType(StrUtil.isNotBlank(command.getSampleType()) ? command.getSampleType() : (task == null ? null : task.getSampleType()));
        sample.setDetectionItems(resolveDetectionItems(command, detectionType));
        sample.setDetectionTypeId(detectionType == null ? command.getDetectionTypeId() : detectionType.getId());
        sample.setDetectionTypeName(resolveDetectionTypeName(command, detectionType));
        sample.setDetectionConfigSnapshot(serializeDetectionConfigItems(detectionConfigItems));
        sample.setSamplingTime(command.getSamplingTime());
        sample.setSealTime(LocalDateTime.now());
        sample.setSamplerId(resolveSamplerId(command, task, currentUser));
        sample.setSamplerName(resolveSamplerName(command, task, currentUser));
        sample.setWeather(command.getWeather());
        sample.setStorageCondition(command.getStorageCondition());
        sample.setSampleStatus(LabWorkflowConstants.SampleStatus.LOGGED);
        sample.setRemark(command.getRemark());
        sample.setTraceLog(buildLoginTrace(sample, task));
        labSampleMapper.insert(sample);

        if (task != null) {
            task.setSampleRegisterStatus(LabWorkflowConstants.SampleRegisterStatus.REGISTERED);
            task.setSampleId(sample.getId());
            task.setSealNo(sample.getSealNo());
            samplingTaskMapper.updateById(task);
        }
        detectionPendingFlowService.createPendingFlowIfMissing(sample);
        return sample;
    }

    public void updateStatus(Long sampleId, String status, String resultSummary) {
        updateStatus(sampleId, status, resultSummary, null);
    }

    public void updateStatus(Long sampleId, String status, String resultSummary, String traceMessage) {
        LabSample sample = labSampleMapper.selectById(sampleId);
        if (sample == null) {
            throw new BusinessException("样品不存在。");
        }
        sample.setSampleStatus(status);
        sample.setResultSummary(resultSummary);
        appendTraceLog(sample, traceMessage);
        labSampleMapper.updateById(sample);
    }

    public void appendTrace(Long sampleId, String traceMessage) {
        if (StrUtil.isBlank(traceMessage)) {
            return;
        }
        LabSample sample = labSampleMapper.selectById(sampleId);
        if (sample == null) {
            throw new BusinessException("样品不存在。");
        }
        appendTraceLog(sample, traceMessage);
        labSampleMapper.updateById(sample);
    }

    private SamplingTask resolveTaskForLogin(SampleLoginCommand command) {
        if (command.getTaskId() != null) {
            SamplingTask task = samplingTaskMapper.selectById(command.getTaskId());
            if (task == null) {
                throw new BusinessException("采样任务不存在。");
            }
            return task;
        }
        String sealNo = normalizeSealNo(command.getSealNo());
        if (StrUtil.isBlank(sealNo)) {
            return null;
        }
        return samplingTaskMapper.selectOne(new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getSealNo, sealNo)
                .last("limit 1"));
    }

    private void validateTaskForSampleLogin(SamplingTask task, CurrentUser currentUser, String sealNo) {
        if (!LabWorkflowConstants.SamplingTaskStatus.COMPLETED.equals(task.getTaskStatus())) {
            throw new BusinessException("采样任务未完成，不能进行样品登录。");
        }
        validateTaskOperator(task, currentUser);
        if (StrUtil.isBlank(task.getSealNo()) && StrUtil.isBlank(sealNo)) {
            throw new BusinessException("采样任务尚未录入封签号，请先录入或粘贴 OCR 识别结果。");
        }
        if (StrUtil.isNotBlank(sealNo) && StrUtil.isNotBlank(task.getSealNo()) && !sealNo.equals(task.getSealNo())) {
            throw new BusinessException("识别到的封签号与采样任务封签号不一致。");
        }
        Long existingCount = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()
                .eq(LabSample::getTaskId, task.getId()));
        if (existingCount != null && existingCount > 0) {
            throw new BusinessException("该采样任务已完成样品登录，不能重复登录。");
        }
    }

    private void validateSealNoUniqueness(String sealNo) {
        if (StrUtil.isBlank(sealNo)) {
            return;
        }
        Long existingCount = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()
                .eq(LabSample::getSealNo, sealNo));
        if (existingCount != null && existingCount > 0) {
            throw new BusinessException("封签号已被占用，请核对后重试。");
        }
    }

    private String resolveSealNo(String commandSealNo, SamplingTask task) {
        if (task != null) {
            if (StrUtil.isNotBlank(task.getSealNo())) {
                return task.getSealNo();
            }
            if (StrUtil.isNotBlank(commandSealNo)) {
                return commandSealNo;
            }
            throw new BusinessException("采样任务尚未录入封签号。");
        }
        if (StrUtil.isBlank(commandSealNo)) {
            throw new BusinessException("无任务直登样品时必须填写封签号。");
        }
        return commandSealNo;
    }

    private String normalizeSealNo(String sealNo) {
        return StrUtil.blankToDefault(StrUtil.trim(sealNo), null);
    }

    private DetectionType resolveDetectionType(SampleLoginCommand command) {
        if (command == null || command.getDetectionTypeId() == null) {
            return null;
        }
        DetectionType detectionType = detectionTypeMapper.selectById(command.getDetectionTypeId());
        if (detectionType == null) {
            throw new BusinessException("检测套餐不存在");
        }
        if (!Integer.valueOf(1).equals(detectionType.getEnabled())) {
            throw new BusinessException("当前检测套餐已停用，不能用于样品登录");
        }
        if (StrUtil.isNotBlank(command.getDetectionTypeName())
                && !StrUtil.equals(command.getDetectionTypeName(), detectionType.getTypeName())) {
            throw new BusinessException("检测套餐名称与配置不一致，请刷新后重试");
        }
        return detectionType;
    }

    private String resolveDetectionItems(SampleLoginCommand command, DetectionType detectionType) {
        if (detectionType != null) {
            return detectionType.getTypeName();
        }
        return StrUtil.trim(command.getDetectionItems());
    }

    private String resolveDetectionTypeName(SampleLoginCommand command, DetectionType detectionType) {
        if (detectionType != null) {
            return detectionType.getTypeName();
        }
        String detectionTypeName = StrUtil.trim(command.getDetectionTypeName());
        if (StrUtil.isNotBlank(detectionTypeName)) {
            return detectionTypeName;
        }
        return StrUtil.trim(command.getDetectionItems());
    }

    private List<SampleDetectionConfigItem> normalizeDetectionConfigItems(SampleLoginCommand command, DetectionType detectionType) {
        if (detectionType == null) {
            return new ArrayList<>();
        }
        List<SampleDetectionConfigItem> items = command.getDetectionConfigItems();
        if (items == null || items.isEmpty()) {
            throw new BusinessException("请选择检测套餐对应的检测参数与检测方法");
        }

        Set<Long> submittedParameterIds = new LinkedHashSet<>();
        List<Long> methodIds = items.stream()
                .map(SampleDetectionConfigItem::getMethodId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, DetectionMethod> methodMap = methodIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : detectionMethodMapper.selectList(new LambdaQueryWrapper<DetectionMethod>()
                        .in(DetectionMethod::getId, methodIds))
                .stream()
                .collect(Collectors.toMap(DetectionMethod::getId, method -> method));

        List<SampleDetectionConfigItem> normalizedItems = new ArrayList<>();
        for (SampleDetectionConfigItem item : items) {
            if (item == null || item.getParameterId() == null) {
                throw new BusinessException("检测参数明细存在空项，请重新选择检测套餐");
            }
            if (!submittedParameterIds.add(item.getParameterId())) {
                throw new BusinessException("样品登录中的检测参数不能重复");
            }
            if (item.getMethodId() == null) {
                throw new BusinessException("请为每个检测参数选择对应的检测方法");
            }
            DetectionMethod method = methodMap.get(item.getMethodId());
            if (method == null) {
                throw new BusinessException("检测方法不存在：" + item.getMethodId());
            }
            if (!item.getParameterId().equals(method.getParameterId())) {
                throw new BusinessException("检测方法“" + method.getMethodName() + "”未绑定到检测参数“"
                        + StrUtil.blankToDefault(item.getParameterName(), String.valueOf(item.getParameterId()))
                        + "”");
            }

            SampleDetectionConfigItem normalizedItem = new SampleDetectionConfigItem();
            normalizedItem.setParameterId(item.getParameterId());
            normalizedItem.setParameterName(StrUtil.blankToDefault(StrUtil.trim(item.getParameterName()), method.getParameterName()));
            normalizedItem.setUnit(StrUtil.trim(item.getUnit()));
            normalizedItem.setStandardMin(item.getStandardMin());
            normalizedItem.setStandardMax(item.getStandardMax());
            normalizedItem.setReferenceStandard(StrUtil.trim(item.getReferenceStandard()));
            normalizedItem.setMethodId(method.getId());
            normalizedItem.setMethodName(method.getMethodName());
            normalizedItems.add(normalizedItem);
        }
        return normalizedItems;
    }

    private String serializeDetectionConfigItems(List<SampleDetectionConfigItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("样品检测参数明细序列化失败");
        }
    }

    private String generateSampleNo() {
        String prefix = DateUtil.format(new Date(), "yyyyMM");
        Long count = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()
                .likeRight(LabSample::getSampleNo, prefix));
        long next = count == null ? 1L : count + 1L;
        return prefix + String.format("%04d", next);
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("当前登录信息已失效，请重新登录。");
        }
        return currentUser;
    }

    private void validateTaskOperator(SamplingTask task, CurrentUser currentUser) {
        if (isAdmin(currentUser)) {
            return;
        }
        if (task.getSamplerId() == null || !task.getSamplerId().equals(currentUser.getUserId())) {
            throw new BusinessException("当前用户不是该采样任务的责任采样员，不能进行样品登录。");
        }
    }

    private void validateSamplerOperator(SampleLoginCommand command, CurrentUser currentUser) {
        if (isAdmin(currentUser)) {
            return;
        }
        if (!currentUser.getUserId().equals(command.getSamplerId())) {
            throw new BusinessException("当前用户只能登录本人采集的样品。");
        }
    }

    private Long resolveSamplerId(SampleLoginCommand command, SamplingTask task, CurrentUser currentUser) {
        if (task != null && task.getSamplerId() != null) {
            return task.getSamplerId();
        }
        return isAdmin(currentUser) ? command.getSamplerId() : currentUser.getUserId();
    }

    private String resolveSamplerName(SampleLoginCommand command, SamplingTask task, CurrentUser currentUser) {
        if (task != null && StrUtil.isNotBlank(task.getSamplerName())) {
            return task.getSamplerName();
        }
        if (isAdmin(currentUser)) {
            return command.getSamplerName();
        }
        return StrUtil.isNotBlank(currentUser.getRealName()) ? currentUser.getRealName() : command.getSamplerName();
    }

    private boolean isAdmin(CurrentUser currentUser) {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRoleCode());
    }

    private String buildLoginTrace(LabSample sample, SamplingTask task) {
        StringBuilder builder = new StringBuilder();
        builder.append(formatTraceEntry("样品登录",
                "封签号=" + sample.getSealNo()
                        + "，样品编号=" + sample.getSampleNo()
                        + "，点位=" + sample.getPointName()
                        + "，采样人=" + sample.getSamplerName()
                        + "，采样时间=" + DateUtil.formatLocalDateTime(sample.getSamplingTime())));
        if (StrUtil.isNotBlank(sample.getStorageCondition())) {
            builder.append("\n").append(formatTraceEntry("样品保存", "存储条件=" + sample.getStorageCondition()));
        }
        if (StrUtil.isNotBlank(sample.getWeather())) {
            builder.append("\n").append(formatTraceEntry("采样环境", "天气=" + sample.getWeather()));
        }
        if (task != null) {
            builder.append("\n").append(formatTraceEntry("来源任务",
                    "采样任务ID=" + task.getId()
                            + "，任务编号=" + StrUtil.blankToDefault(task.getTaskNo(), "-")
                            + "，封签号=" + StrUtil.blankToDefault(task.getSealNo(), "-")));
        }
        return builder.toString();
    }

    private void appendTraceLog(LabSample sample, String traceMessage) {
        if (sample == null || StrUtil.isBlank(traceMessage)) {
            return;
        }
        String entry = formatTraceEntry("流程留痕", traceMessage);
        if (StrUtil.isBlank(sample.getTraceLog())) {
            sample.setTraceLog(entry);
            return;
        }
        sample.setTraceLog(sample.getTraceLog() + "\n" + entry);
    }

    private String formatTraceEntry(String title, String content) {
        return DateUtil.formatDateTime(new Date()) + " [" + title + "] " + StrUtil.trim(content);
    }
}
