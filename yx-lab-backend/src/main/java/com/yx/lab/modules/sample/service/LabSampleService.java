package com.yx.lab.modules.sample.service;

import cn.hutool.core.date.DateUtil;
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
import com.yx.lab.modules.detection.dto.DetectionTypeParameterMethodBindingItem;
import com.yx.lab.modules.detection.entity.DetectionMethod;
import com.yx.lab.modules.detection.entity.DetectionParameter;
import com.yx.lab.modules.detection.entity.DetectionType;
import com.yx.lab.modules.detection.mapper.DetectionMethodMapper;
import com.yx.lab.modules.detection.mapper.DetectionParameterMapper;
import com.yx.lab.modules.detection.mapper.DetectionTypeMapper;
import com.yx.lab.modules.detection.service.DetectionPendingFlowService;
import com.yx.lab.modules.sample.dto.LabSampleQuery;
import com.yx.lab.modules.sample.dto.SampleDetectionConfigItem;
import com.yx.lab.modules.sample.dto.SampleLoginCommand;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.sample.mapper.SamplingTaskMapper;
import com.yx.lab.modules.sample.vo.StatusCountVO;
import com.yx.lab.modules.system.entity.LabFlowConfig;
import com.yx.lab.modules.system.mapper.LabFlowConfigMapper;
import com.yx.lab.modules.system.service.FlowConfigManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabSampleService {

    private final LabSampleMapper labSampleMapper;

    private final SamplingTaskMapper samplingTaskMapper;

    private final DetectionTypeMapper detectionTypeMapper;

    private final DetectionMethodMapper detectionMethodMapper;

    private final DetectionParameterMapper detectionParameterMapper;

    private final DetectionPendingFlowService detectionPendingFlowService;

    private final LabFlowConfigMapper labFlowConfigMapper;

    private final ObjectMapper objectMapper;

    private final DataScopeHelper dataScopeHelper;

    private final SamplingPlanService samplingPlanService;

    /**
     * 分页查询样品列表。
     *
     * @param query 查询条件
     * @return 样品分页结果
     */
    public PageResult<LabSample> page(LabSampleQuery query) {
        Page<LabSample> page = labSampleMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<LabSample>()
                        .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                                .like(LabSample::getSampleNo, query.getKeyword())
                                .or()
                                .like(LabSample::getPointName, query.getKeyword()))
                        .eq(StrUtil.isNotBlank(query.getSampleStatus()), LabSample::getSampleStatus, query.getSampleStatus())
                        .eq(StrUtil.isNotBlank(query.getSampleType()), LabSample::getSampleType, query.getSampleType())
                        .eq(StrUtil.isNotBlank(query.getSampleSourceMethod()), LabSample::getSampleSourceMethod, query.getSampleSourceMethod())
                        .eq(resolveScopedSamplerId() != null, LabSample::getSamplerId, resolveScopedSamplerId())
                        .eq(dataScopeHelper.onlySelfScope(), LabSample::getCreatedBy, dataScopeHelper.currentUserId())
                        .orderByDesc(LabSample::getCreatedTime));
        page.getRecords().forEach(this::enrichSampleDetectionConfigSnapshotForView);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    /**
     * 按样品状态统计当前用户可见范围内的样品数量。
     *
     * @return 状态数量列表
     */
    public List<StatusCountVO> statusStats() {
        return Arrays.asList(
                statusCount("ALL", countSamplesByStatus(null)),
                statusCount(LabWorkflowConstants.SampleStatus.LOGGED,
                        countSamplesByStatus(LabWorkflowConstants.SampleStatus.LOGGED)),
                statusCount(LabWorkflowConstants.SampleStatus.REVIEWING,
                        countSamplesByStatus(LabWorkflowConstants.SampleStatus.REVIEWING)),
                statusCount(LabWorkflowConstants.SampleStatus.RETEST,
                        countSamplesByStatus(LabWorkflowConstants.SampleStatus.RETEST)),
                statusCount(LabWorkflowConstants.SampleStatus.COMPLETED,
                        countSamplesByStatus(LabWorkflowConstants.SampleStatus.COMPLETED)));
    }

    private StatusCountVO statusCount(String status, Long count) {
        StatusCountVO vo = new StatusCountVO();
        vo.setStatus(status);
        vo.setCount(count == null ? 0L : count);
        return vo;
    }

    private Long countSamplesByStatus(String sampleStatus) {
        Number count = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()
                .eq(StrUtil.isNotBlank(sampleStatus), LabSample::getSampleStatus, sampleStatus)
                .eq(resolveScopedSamplerId() != null, LabSample::getSamplerId, resolveScopedSamplerId())
                .eq(dataScopeHelper.onlySelfScope(), LabSample::getCreatedBy, dataScopeHelper.currentUserId()));
        return count == null ? 0L : count.longValue();
    }

    private Long resolveScopedSamplerId() {
        if (dataScopeHelper.isAdmin()) {
            return null;
        }
        if (dataScopeHelper.isRole("STAFF") && dataScopeHelper.currentUserId() != null) {
            return dataScopeHelper.currentUserId();
        }
        return null;
    }

    /**
     * 获取样品详情。
     *
     * @param id 样品ID
     * @return 样品详情
     */
    public LabSample detail(Long id) {
        LabSample sample = labSampleMapper.selectById(id);
        enrichSampleDetectionConfigSnapshotForView(sample);
        return sample;
    }

    /**
     * 执行样品登录，并同步任务状态与待检流程。
     *
     * @param command 样品登录参数
     * @return 登录后的样品记录
     */
    @Transactional(rollbackFor = Exception.class)
    public LabSample loginSample(SampleLoginCommand command) {
        CurrentUser currentUser = requireCurrentUser();
        SamplingTask task = resolveTaskForLogin(command);
        validateTaskForSampleLogin(task, currentUser);
        // 任务来源样品的套餐由采样计划派发时固化，样品登录只做回显和承接，不能再重新选择套餐。
        DetectionType detectionType = resolveDetectionType(command, task);
        List<SampleDetectionConfigItem> detectionConfigItems = normalizeDetectionConfigItems(command, task, detectionType);
        LabFlowConfig reviewFlow = resolveSelectedFlow(
                command.getReviewFlowId(),
                command.getReviewFlowName(),
                FlowConfigManagementService.FLOW_TYPE_REVIEW,
                "审核流程");

        String sampleSourceMethod = resolveSampleSourceMethod(command);

        LabSample sample = new LabSample();
        sample.setSampleNo(StrUtil.trim(task.getSampleNo()));
        sample.setTaskId(task.getId());
        sample.setPointId(command.getPointId() != null ? command.getPointId() : task.getPointId());
        sample.setPointName(StrUtil.isNotBlank(command.getPointName()) ? command.getPointName() : task.getPointName());
        sample.setSampleType(StrUtil.isNotBlank(command.getSampleType()) ? command.getSampleType() : task.getSampleType());
        sample.setSampleSourceMethod(sampleSourceMethod);
        sample.setDetectionItems(resolveDetectionItems(command, task, detectionType));
        sample.setDetectionTypeId(resolveDetectionTypeId(command, task, detectionType));
        sample.setDetectionTypeName(resolveDetectionTypeName(command, task, detectionType));
        sample.setDetectionConfigSnapshot(samplingPlanService.enrichDetectionConfigSnapshotForView(
                serializeDetectionConfigItems(detectionConfigItems)));
        sample.setReviewFlowId(reviewFlow == null ? null : reviewFlow.getId());
        sample.setReviewFlowName(reviewFlow == null ? null : reviewFlow.getFlowName());
        sample.setSamplingTime(command.getSamplingTime());
        sample.setSampleTotalVolume(task.getSampleTotalVolume());
        sample.setSampleBottleCount(task.getSampleBottleCount());
        sample.setSamplerId(resolveSamplerId(command, task, currentUser));
        sample.setSamplerName(resolveSamplerName(command, task, currentUser));
        sample.setWeather(command.getWeather());
        sample.setStorageCondition(command.getStorageCondition());
        sample.setSampleStatus(LabWorkflowConstants.SampleStatus.LOGGED);
        sample.setRemark(command.getRemark());
        sample.setTraceLog(buildLoginTrace(sample, task));
        labSampleMapper.insert(sample);

        task.setSampleRegisterStatus(LabWorkflowConstants.SampleRegisterStatus.REGISTERED);
        task.setSampleId(sample.getId());
        samplingTaskMapper.updateById(task);
        abandonOtherTasksInSamePlan(task, currentUser);
        // 样品一旦登录完成，立即补齐后续待分配检测主流程与参数子流程。
        detectionPendingFlowService.createPendingFlowIfMissing(sample);
        enrichSampleDetectionConfigSnapshotForView(sample);
        return sample;
    }

    private void abandonOtherTasksInSamePlan(SamplingTask completedTask, CurrentUser currentUser) {
        if (completedTask == null || completedTask.getPlanId() == null || completedTask.getId() == null) {
            return;
        }
        String abandonReason = resolveCompletedOperatorName(currentUser, completedTask) + "已完成";
        List<SamplingTask> siblingTasks = samplingTaskMapper.selectList(new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getPlanId, completedTask.getPlanId())
                .ne(SamplingTask::getId, completedTask.getId())
                .ne(SamplingTask::getTaskStatus, LabWorkflowConstants.SamplingTaskStatus.ABANDONED)
                .isNull(SamplingTask::getSampleId)
                .and(wrapper -> wrapper
                        .isNull(SamplingTask::getSampleRegisterStatus)
                        .or()
                        .ne(SamplingTask::getSampleRegisterStatus, LabWorkflowConstants.SampleRegisterStatus.REGISTERED)));
        for (SamplingTask siblingTask : siblingTasks) {
            siblingTask.setTaskStatus(LabWorkflowConstants.SamplingTaskStatus.ABANDONED);
            siblingTask.setAbandonReason(abandonReason);
            samplingTaskMapper.updateById(siblingTask);
        }
        if (!siblingTasks.isEmpty()) {
            samplingPlanService.refreshPlanStatusAfterTaskChange(completedTask.getPlanId());
        }
    }

    private String resolveCompletedOperatorName(CurrentUser currentUser, SamplingTask completedTask) {
        String operatorName = currentUser == null
                ? null
                : StrUtil.blankToDefault(StrUtil.trim(currentUser.getRealName()), StrUtil.trim(currentUser.getUsername()));
        return StrUtil.blankToDefault(operatorName, StrUtil.blankToDefault(StrUtil.trim(completedTask.getSamplerName()), "采样员"));
    }

    private void enrichSampleDetectionConfigSnapshotForView(LabSample sample) {
        if (sample != null) {
            sample.setDetectionConfigSnapshot(samplingPlanService.enrichDetectionConfigSnapshotForView(sample.getDetectionConfigSnapshot()));
        }
    }

    /**
     * 更新样品状态与结果摘要。
     *
     * @param sampleId 样品ID
     * @param status 样品状态
     * @param resultSummary 结果摘要
     */
    public void updateStatus(Long sampleId, String status, String resultSummary) {
        updateStatus(sampleId, status, resultSummary, null);
    }

    /**
     * 更新样品状态、结果摘要，并可追加一条流程留痕。
     *
     * @param sampleId 样品ID
     * @param status 样品状态
     * @param resultSummary 结果摘要
     * @param traceMessage 流程留痕内容
     */
    public void updateStatus(Long sampleId, String status, String resultSummary, String traceMessage) {
        LabSample sample = labSampleMapper.selectById(sampleId);
        if (sample == null) {
            throw new BusinessException("样品不存在。");
        }
        sample.setSampleStatus(status);
        sample.setResultSummary(LabWorkflowConstants.getDetectionResultLabel(resultSummary));
        appendTraceLog(sample, traceMessage);
        labSampleMapper.updateById(sample);
    }

    /**
     * 追加样品流程留痕。
     *
     * @param sampleId 样品ID
     * @param traceMessage 留痕内容
     */
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
        if (command.getTaskId() == null) {
            throw new BusinessException("请选择已完成采样的任务后再进行样品登录。");
        }
        SamplingTask task = samplingTaskMapper.selectById(command.getTaskId());
        if (task == null) {
            throw new BusinessException("采样任务不存在。");
        }
        return task;
    }

    private void validateTaskForSampleLogin(SamplingTask task, CurrentUser currentUser) {
        if (!LabWorkflowConstants.SamplingTaskStatus.COMPLETED.equals(task.getTaskStatus())) {
            throw new BusinessException("采样任务未完成，不能进行样品登录。");
        }
        validateTaskOperator(task, currentUser);
        if (StrUtil.isBlank(task.getSampleNo())) {
            throw new BusinessException("采样任务尚未生成样品编号，请先完成采样录入。");
        }
        Number existingCount = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()
                .eq(LabSample::getTaskId, task.getId()));
        if (existingCount != null && existingCount.longValue() > 0) {
            throw new BusinessException("该采样任务已完成样品登录，不能重复登录。");
        }
    }

    private String resolveSampleSourceMethod(SampleLoginCommand command) {
        String sampleSourceMethod = StrUtil.blankToDefault(
                StrUtil.trim(command.getSampleSourceMethod()),
                LabWorkflowConstants.SampleSourceMethod.SAMPLING);
        if (!LabWorkflowConstants.SAMPLE_SOURCE_METHODS.contains(sampleSourceMethod)) {
            throw new BusinessException("样品来源方式不正确");
        }
        return sampleSourceMethod;
    }

    private DetectionType resolveDetectionType(SampleLoginCommand command, SamplingTask task) {
        Long detectionTypeId = command.getDetectionTypeId();
        String detectionTypeName = command.getDetectionTypeName();
        if (detectionTypeId == null) {
            throw new BusinessException("请选择检测套餐");
        }
        DetectionType detectionType = detectionTypeMapper.selectById(detectionTypeId);
        if (detectionType == null) {
            throw new BusinessException("检测套餐不存在");
        }
        if (!Integer.valueOf(1).equals(detectionType.getEnabled())) {
            throw new BusinessException("当前检测套餐已停用，不能用于样品登录");
        }
        String sampleType = StrUtil.isNotBlank(command.getSampleType()) ? StrUtil.trim(command.getSampleType()) : task.getSampleType();
        if (StrUtil.isNotBlank(detectionType.getSampleType()) && !StrUtil.equals(detectionType.getSampleType(), sampleType)) {
            throw new BusinessException("检测套餐与样品类型不匹配，请重新选择");
        }
        if (StrUtil.isNotBlank(detectionTypeName)
                && !StrUtil.equals(StrUtil.trim(detectionTypeName), detectionType.getTypeName())) {
            throw new BusinessException("检测套餐名称与配置不一致，请刷新后重试");
        }
        return detectionType;
    }

    private String resolveDetectionItems(SampleLoginCommand command, SamplingTask task, DetectionType detectionType) {
        if (detectionType != null) {
            return detectionType.getTypeName();
        }
        return StrUtil.trim(command.getDetectionItems());
    }

    private Long resolveDetectionTypeId(SampleLoginCommand command, SamplingTask task, DetectionType detectionType) {
        return detectionType == null ? command.getDetectionTypeId() : detectionType.getId();
    }

    private String resolveDetectionTypeName(SampleLoginCommand command, SamplingTask task, DetectionType detectionType) {
        if (detectionType != null) {
            return detectionType.getTypeName();
        }
        String detectionTypeName = StrUtil.trim(command.getDetectionTypeName());
        if (StrUtil.isNotBlank(detectionTypeName)) {
            return detectionTypeName;
        }
        return StrUtil.trim(command.getDetectionItems());
    }
    private LabFlowConfig resolveSelectedFlow(Long flowId, String submittedFlowName, String expectedType, String label) {
        LabFlowConfig flow = flowId == null ? findDefaultFlow(expectedType) : labFlowConfigMapper.selectById(flowId);
        if (flow == null) {
            return null;
        }
        if (!expectedType.equals(flow.getFlowType())) {
            throw new BusinessException(label + "类型不正确，请刷新后重新选择");
        }
        if (!Integer.valueOf(1).equals(flow.getStatus())) {
            throw new BusinessException(label + "已停用，请重新选择");
        }
        if (StrUtil.isNotBlank(submittedFlowName) && !StrUtil.equals(StrUtil.trim(submittedFlowName), flow.getFlowName())) {
            throw new BusinessException(label + "名称与配置不一致，请刷新后重新选择");
        }
        return flow;
    }

    private LabFlowConfig findDefaultFlow(String flowType) {
        return labFlowConfigMapper.selectOne(new LambdaQueryWrapper<LabFlowConfig>()
                .eq(LabFlowConfig::getFlowType, flowType)
                .eq(LabFlowConfig::getStatus, 1)
                .orderByDesc(LabFlowConfig::getDefaultFlag)
                .orderByAsc(LabFlowConfig::getFlowName)
                .last("limit 1"));
    }

    private List<SampleDetectionConfigItem> normalizeDetectionConfigItems(SampleLoginCommand command,
                                                                          SamplingTask task,
                                                                          DetectionType detectionType) {
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
            normalizedItem.setSampleVolume(method.getSampleVolume());
            normalizedItem.setMethodBasis(method.getMethodBasis());
            normalizedItems.add(normalizedItem);
        }
        return normalizedItems;
    }

    private List<SampleDetectionConfigItem> parseDetectionConfigSnapshot(String snapshotText) {
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
                    .filter(item -> item != null && item.getParameterId() != null && item.getMethodId() != null)
                    .collect(Collectors.toList());
        } catch (JsonProcessingException ex) {
            throw new BusinessException("采样任务检测套餐参数快照格式错误，不能进行样品登录");
        }
    }

    private List<SampleDetectionConfigItem> buildDetectionConfigItems(DetectionType detectionType) {
        List<Long> parameterIds = parseIdList(detectionType.getParameterIds());
        if (parameterIds.isEmpty()) {
            throw new BusinessException("当前检测套餐未配置检测参数，不能用于样品登录");
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
                .map(parameterId -> buildDetectionConfigItem(parameterId, parameterMap.get(parameterId), methodMap.get(methodIdByParameterId.get(parameterId))))
                .collect(Collectors.toList());
    }

    private SampleDetectionConfigItem buildDetectionConfigItem(Long parameterId,
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
        if (!isSamplerAssigned(task, currentUser.getUserId())) {
            throw new BusinessException("当前用户不是该采样任务的责任采样员，不能进行样品登录。");
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

    private boolean isSamplerAssigned(SamplingTask task, Long samplerId) {
        if (task == null || samplerId == null) {
            return false;
        }
        if (samplerId.equals(task.getSamplerId())) {
            return true;
        }
        return StrUtil.contains(task.getSamplerIds(), "," + samplerId + ",");
    }

    private boolean isAdmin(CurrentUser currentUser) {
        return currentUser != null
                && ("ADMIN".equalsIgnoreCase(currentUser.getRoleCode())
                || "DIRECTOR".equalsIgnoreCase(currentUser.getRoleCode()));
    }

    private String buildLoginTrace(LabSample sample, SamplingTask task) {
        StringBuilder builder = new StringBuilder();
        builder.append(formatTraceEntry("样品登录",
                "样品编号=" + sample.getSampleNo()
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
                            + "，样品编号=" + StrUtil.blankToDefault(task.getSampleNo(), "-")));
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
