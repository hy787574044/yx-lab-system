package com.yx.lab.modules.detection.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.detection.dto.DetectionMethodQuery;
import com.yx.lab.modules.detection.dto.DetectionMethodSaveCommand;
import com.yx.lab.modules.detection.dto.DetectionParameterQuery;
import com.yx.lab.modules.detection.dto.DetectionParameterMethodBindCommand;
import com.yx.lab.modules.detection.dto.DetectionParameterSaveCommand;
import com.yx.lab.modules.detection.dto.DetectionProjectGroupQuery;
import com.yx.lab.modules.detection.dto.DetectionProjectGroupSaveCommand;
import com.yx.lab.modules.detection.dto.DetectionStepQuery;
import com.yx.lab.modules.detection.dto.DetectionStepSaveCommand;
import com.yx.lab.modules.detection.dto.DetectionTypeQuery;
import com.yx.lab.modules.detection.dto.DetectionTypeParameterMethodBindingItem;
import com.yx.lab.modules.detection.dto.DetectionTypeSaveCommand;
import com.yx.lab.modules.detection.entity.DetectionItem;
import com.yx.lab.modules.detection.entity.DetectionMethod;
import com.yx.lab.modules.detection.entity.DetectionParameter;
import com.yx.lab.modules.detection.entity.DetectionProjectGroup;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.entity.DetectionStep;
import com.yx.lab.modules.detection.entity.DetectionType;
import com.yx.lab.modules.detection.mapper.DetectionItemMapper;
import com.yx.lab.modules.detection.mapper.DetectionMethodMapper;
import com.yx.lab.modules.detection.mapper.DetectionParameterMapper;
import com.yx.lab.modules.detection.mapper.DetectionProjectGroupMapper;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.detection.mapper.DetectionStepMapper;
import com.yx.lab.modules.detection.mapper.DetectionTypeMapper;
import com.yx.lab.modules.detection.vo.DetectionDetectorOptionVO;
import com.yx.lab.modules.detection.vo.DetectionParameterMethodBindingVO;
import com.yx.lab.modules.system.entity.LabUser;
import com.yx.lab.modules.system.mapper.LabUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 检测配置服务。
 */
@Service
@RequiredArgsConstructor
public class DetectionConfigService {

    private static final String DETECTOR_ROLE_CODE = "DETECTOR";

    private static final String SUBMITTED_DETECTION_STATUS = "SUBMITTED";

    private final DetectionTypeMapper detectionTypeMapper;

    private final DetectionParameterMapper detectionParameterMapper;

    private final DetectionMethodMapper detectionMethodMapper;

    private final DetectionProjectGroupMapper detectionProjectGroupMapper;

    private final DetectionStepMapper detectionStepMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    private final DetectionItemMapper detectionItemMapper;

    private final LabUserMapper labUserMapper;

    private final ObjectMapper objectMapper;

    public PageResult<DetectionType> typePage(DetectionTypeQuery query) {
        String keyword = StrUtil.trim(query.getKeyword());
        Page<DetectionType> page = detectionTypeMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<DetectionType>()
                        .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                                .like(DetectionType::getTypeName, keyword)
                                .or()
                                .like(DetectionType::getGroupName, keyword)
                                .or()
                                .like(DetectionType::getDetectorName, keyword))
                        .eq(query.getGroupId() != null, DetectionType::getGroupId, query.getGroupId())
                        .eq(query.getDetectorId() != null, DetectionType::getDetectorId, query.getDetectorId())
                        .eq(query.getEnabled() != null, DetectionType::getEnabled, query.getEnabled())
                        .orderByDesc(DetectionType::getCreatedTime));
        fillTypeParameterMethodNames(page.getRecords());
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public PageResult<DetectionProjectGroup> projectGroupPage(DetectionProjectGroupQuery query) {
        String keyword = StrUtil.trim(query.getKeyword());
        Page<DetectionProjectGroup> page = detectionProjectGroupMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<DetectionProjectGroup>()
                        .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                                .like(DetectionProjectGroup::getGroupName, keyword)
                                .or()
                                .like(DetectionProjectGroup::getRemark, keyword))
                        .eq(query.getEnabled() != null, DetectionProjectGroup::getEnabled, query.getEnabled())
                        .orderByDesc(DetectionProjectGroup::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public List<DetectionDetectorOptionVO> detectorOptions() {
        return labUserMapper.selectList(new LambdaQueryWrapper<LabUser>()
                        .eq(LabUser::getStatus, 1)
                        .eq(LabUser::getRoleCode, DETECTOR_ROLE_CODE)
                        .orderByAsc(LabUser::getRealName)
                        .orderByAsc(LabUser::getUsername))
                .stream()
                .map(this::toDetectorOption)
                .collect(Collectors.toList());
    }

    public PageResult<DetectionParameter> parameterPage(DetectionParameterQuery query) {
        String keyword = StrUtil.trim(query.getKeyword());
        Page<DetectionParameter> page = detectionParameterMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<DetectionParameter>()
                        .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                                .like(DetectionParameter::getParameterName, keyword)
                                .or()
                                .like(DetectionParameter::getUnit, keyword)
                                .or()
                                .like(DetectionParameter::getReferenceStandard, keyword)
                                .or()
                                .like(DetectionParameter::getRemark, keyword))
                        .orderByDesc(DetectionParameter::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public PageResult<DetectionParameterMethodBindingVO> parameterMethodBindingPage(DetectionParameterQuery query) {
        PageResult<DetectionParameter> parameterPage = parameterPage(query);
        List<DetectionParameter> parameters = parameterPage.getRecords();
        if (parameters == null || parameters.isEmpty()) {
            return new PageResult<>(parameterPage.getTotal(), new ArrayList<>());
        }

        List<Long> parameterIds = parameters.stream()
                .map(DetectionParameter::getId)
                .collect(Collectors.toList());

        Map<Long, List<DetectionMethod>> methodMap = detectionMethodMapper.selectList(new LambdaQueryWrapper<DetectionMethod>()
                        .in(DetectionMethod::getParameterId, parameterIds)
                        .orderByAsc(DetectionMethod::getMethodName)
                        .orderByAsc(DetectionMethod::getMethodCode))
                .stream()
                .collect(Collectors.groupingBy(DetectionMethod::getParameterId, LinkedHashMap::new, Collectors.toList()));

        List<DetectionParameterMethodBindingVO> records = parameters.stream()
                .map(parameter -> toParameterMethodBindingVO(parameter, methodMap.get(parameter.getId())))
                .collect(Collectors.toList());
        return new PageResult<>(parameterPage.getTotal(), records);
    }

    public List<DetectionMethod> methodOptions() {
        return detectionMethodMapper.selectList(new LambdaQueryWrapper<DetectionMethod>()
                .orderByAsc(DetectionMethod::getMethodName)
                .orderByAsc(DetectionMethod::getMethodCode));
    }

    public PageResult<DetectionMethod> methodPage(DetectionMethodQuery query) {
        String keyword = StrUtil.trim(query.getKeyword());
        Page<DetectionMethod> page = detectionMethodMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<DetectionMethod>()
                        .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                                .like(DetectionMethod::getMethodName, keyword)
                                .or()
                                .like(DetectionMethod::getMethodCode, keyword)
                                .or()
                                .like(DetectionMethod::getParameterName, keyword)
                                .or()
                                .like(DetectionMethod::getStandardCode, keyword)
                                .or()
                                .like(DetectionMethod::getMethodBasis, keyword)
                                .or()
                                .like(DetectionMethod::getApplyScope, keyword)
                                .or()
                                .like(DetectionMethod::getRemark, keyword))
                        .orderByDesc(DetectionMethod::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public PageResult<DetectionStep> stepPage(DetectionStepQuery query) {
        Page<DetectionStep> page = detectionStepMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<DetectionStep>()
                        .eq(query.getTypeId() != null, DetectionStep::getTypeId, query.getTypeId())
                        .orderByAsc(DetectionStep::getStepOrder));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveType(DetectionTypeSaveCommand command) {
        DetectionType entity = new DetectionType();
        applyTypeCommand(entity, command);
        validateType(entity, null, null);
        detectionTypeMapper.insert(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateType(Long id, DetectionTypeSaveCommand command) {
        DetectionType entity = requireType(id);
        Long previousDetectorId = entity.getDetectorId();
        applyTypeCommand(entity, command);
        validateType(entity, id, previousDetectorId);
        detectionTypeMapper.updateById(entity);
        syncStepTypeName(entity);
    }

    public void deleteType(Long id) {
        DetectionType entity = requireType(id);
        Long stepCount = detectionStepMapper.selectCount(new LambdaQueryWrapper<DetectionStep>()
                .eq(DetectionStep::getTypeId, entity.getId()));
        if (stepCount != null && stepCount > 0) {
            throw new BusinessException("当前检测套餐已配置检测步骤，不能删除");
        }
        Long recordCount = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionTypeId, entity.getId()));
        if (recordCount != null && recordCount > 0) {
            throw new BusinessException("当前检测套餐已产生检测记录，不能删除");
        }
        detectionTypeMapper.deleteById(entity.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveProjectGroup(DetectionProjectGroupSaveCommand command) {
        DetectionProjectGroup entity = new DetectionProjectGroup();
        applyProjectGroupCommand(entity, command);
        validateProjectGroup(entity, null);
        detectionProjectGroupMapper.insert(entity);
        syncProjectGroupProjects(entity, command.getProjectIds());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProjectGroup(Long id, DetectionProjectGroupSaveCommand command) {
        DetectionProjectGroup entity = requireProjectGroup(id);
        applyProjectGroupCommand(entity, command);
        validateProjectGroup(entity, id);
        detectionProjectGroupMapper.updateById(entity);
        syncProjectGroupProjects(entity, command.getProjectIds());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProjectGroup(Long id) {
        DetectionProjectGroup entity = requireProjectGroup(id);
        clearProjectGroupBindings(entity.getId());
        detectionProjectGroupMapper.deleteById(entity.getId());
    }

    public void saveParameter(DetectionParameterSaveCommand command) {
        DetectionParameter entity = new DetectionParameter();
        applyParameterCommand(entity, command);
        validateParameter(entity, null);
        detectionParameterMapper.insert(entity);
    }

    public void updateParameter(Long id, DetectionParameterSaveCommand command) {
        DetectionParameter entity = requireParameter(id);
        applyParameterCommand(entity, command);
        validateParameter(entity, id);
        detectionParameterMapper.updateById(entity);
        syncTypeParameterSnapshots(id);
        syncMethodParameterSnapshots(id, entity.getParameterName());
    }

    public void deleteParameter(Long id) {
        DetectionParameter entity = requireParameter(id);
        if (isParameterReferencedByType(entity.getId())) {
            throw new BusinessException("当前检测参数已被检测套餐引用，不能删除");
        }
        Long methodCount = detectionMethodMapper.selectCount(new LambdaQueryWrapper<DetectionMethod>()
                .eq(DetectionMethod::getParameterId, entity.getId()));
        if (methodCount != null && methodCount > 0) {
            throw new BusinessException("当前检测参数已绑定检测方法，请先解除绑定后再删除");
        }
        Long itemCount = detectionItemMapper.selectCount(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getParameterId, entity.getId()));
        if (itemCount != null && itemCount > 0) {
            throw new BusinessException("当前检测参数已产生检测结果记录，不能删除");
        }
        detectionParameterMapper.deleteById(entity.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindParameterMethods(Long parameterId, DetectionParameterMethodBindCommand command) {
        DetectionParameter parameter = requireParameter(parameterId);
        List<Long> methodIds = normalizeMethodIds(command == null ? null : command.getMethodIds());
        ensureRemovedMethodsNotReferenced(parameterId, methodIds);
        Map<Long, DetectionMethod> selectedMethodMap = resolveSelectedMethods(methodIds);

        for (Long methodId : methodIds) {
            DetectionMethod method = selectedMethodMap.get(methodId);
            if (method.getParameterId() != null && !parameterId.equals(method.getParameterId())) {
                throw new BusinessException("检测方法“" + method.getMethodName() + "”已绑定到参数“"
                        + StrUtil.blankToDefault(method.getParameterName(), String.valueOf(method.getParameterId()))
                        + "”，请先解除原绑定");
            }
        }

        clearMethodBindingsByParameter(parameterId);
        for (Long methodId : methodIds) {
            DetectionMethod method = selectedMethodMap.get(methodId);
            method.setParameterId(parameter.getId());
            method.setParameterName(parameter.getParameterName());
            detectionMethodMapper.updateById(method);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveMethod(DetectionMethodSaveCommand command) {
        DetectionMethod entity = new DetectionMethod();
        applyMethodCommand(entity, command);
        validateMethod(entity, null);
        detectionMethodMapper.insert(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateMethod(Long id, DetectionMethodSaveCommand command) {
        DetectionMethod entity = requireMethod(id);
        applyMethodCommand(entity, command);
        validateMethod(entity, id);
        detectionMethodMapper.updateById(entity);
        syncTypeMethodBindingsByMethodId(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteMethod(Long id) {
        DetectionMethod entity = requireMethod(id);
        if (isMethodReferencedByType(entity.getId())) {
            throw new BusinessException("当前检测方法已被检测套餐引用，请先调整检测套餐后再删除");
        }
        detectionMethodMapper.deleteById(entity.getId());
    }

    public void saveStep(DetectionStepSaveCommand command) {
        DetectionStep entity = new DetectionStep();
        applyStepCommand(entity, command);
        validateStep(entity, null);
        detectionStepMapper.insert(entity);
    }

    public void updateStep(Long id, DetectionStepSaveCommand command) {
        DetectionStep entity = requireStep(id);
        applyStepCommand(entity, command);
        validateStep(entity, id);
        detectionStepMapper.updateById(entity);
    }

    public void deleteStep(Long id) {
        detectionStepMapper.deleteById(requireStep(id).getId());
    }

    private DetectionType requireType(Long id) {
        DetectionType entity = detectionTypeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("检测套餐不存在");
        }
        return entity;
    }

    private DetectionParameter requireParameter(Long id) {
        DetectionParameter entity = detectionParameterMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("检测参数不存在");
        }
        return entity;
    }

    private DetectionMethod requireMethod(Long id) {
        DetectionMethod entity = detectionMethodMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("检测方法不存在");
        }
        return entity;
    }

    private DetectionStep requireStep(Long id) {
        DetectionStep entity = detectionStepMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("检测步骤不存在");
        }
        return entity;
    }

    private DetectionProjectGroup requireProjectGroup(Long id) {
        DetectionProjectGroup entity = detectionProjectGroupMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("检测项目组不存在");
        }
        return entity;
    }

    private void applyTypeCommand(DetectionType entity, DetectionTypeSaveCommand command) {
        entity.setTypeName(StrUtil.trim(command.getTypeName()));
        applyProjectGroupToType(entity, command.getGroupId());
        applyDetectorToType(entity, command.getDetectorId());
        entity.setParameterIds(normalizeIdList(command.getParameterIds()));
        entity.setParameterNames(StrUtil.trim(command.getParameterNames()));
        if (command.getParameterMethodBindings() != null) {
            entity.setParameterMethodBindings(serializeTypeParameterMethodBindings(command.getParameterMethodBindings()));
        } else if (StrUtil.isNotBlank(command.getParameterIds()) || StrUtil.isNotBlank(command.getParameterNames())) {
            entity.setParameterMethodBindings(null);
        }
        entity.setParameterMethodNames(null);
        entity.setEnabled(command.getEnabled());
        entity.setRemark(StrUtil.trim(command.getRemark()));
    }

    private void applyProjectGroupCommand(DetectionProjectGroup entity, DetectionProjectGroupSaveCommand command) {
        entity.setGroupName(StrUtil.trim(command.getGroupName()));
        entity.setEnabled(command.getEnabled());
        entity.setRemark(StrUtil.trim(command.getRemark()));
    }

    private void applyParameterCommand(DetectionParameter entity, DetectionParameterSaveCommand command) {
        entity.setParameterName(StrUtil.trim(command.getParameterName()));
        entity.setStandardMin(command.getStandardMin());
        entity.setStandardMax(command.getStandardMax());
        entity.setUnit(StrUtil.trim(command.getUnit()));
        entity.setExceedRule(StrUtil.trim(command.getExceedRule()));
        entity.setReferenceStandard(StrUtil.trim(command.getReferenceStandard()));
        entity.setEnabled(command.getEnabled());
        entity.setRemark(StrUtil.trim(command.getRemark()));
    }

    private void applyMethodCommand(DetectionMethod entity, DetectionMethodSaveCommand command) {
        entity.setMethodName(StrUtil.trim(command.getMethodName()));
        entity.setMethodCode(StrUtil.trim(command.getMethodCode()));
        entity.setStandardCode(StrUtil.trim(command.getStandardCode()));
        entity.setMethodBasis(StrUtil.trim(command.getMethodBasis()));
        entity.setApplyScope(StrUtil.trim(command.getApplyScope()));
        entity.setEnabled(command.getEnabled());
        entity.setRemark(StrUtil.trim(command.getRemark()));
    }

    private void applyStepCommand(DetectionStep entity, DetectionStepSaveCommand command) {
        entity.setTypeId(command.getTypeId());
        entity.setTypeName(StrUtil.trim(command.getTypeName()));
        entity.setStepName(StrUtil.trim(command.getStepName()));
        entity.setStepOrder(command.getStepOrder());
        entity.setStepDescription(StrUtil.trim(command.getStepDescription()));
        entity.setReagentRequirement(StrUtil.trim(command.getReagentRequirement()));
        entity.setOperationRequirement(StrUtil.trim(command.getOperationRequirement()));
        entity.setRemark(StrUtil.trim(command.getRemark()));
    }

    private void validateType(DetectionType entity, Long selfId, Long previousDetectorId) {
        validateEnabledFlag(entity.getEnabled(), "检测套餐启用状态不合法");
        ensureTypeNameUnique(entity.getTypeName(), selfId);
        validateAndPopulateTypeParameters(entity);
        if (selfId != null && !equalsNullableLong(previousDetectorId, entity.getDetectorId()) && hasProcessingDetections(selfId)) {
            throw new BusinessException("当前检测套餐存在检测中的记录，暂不支持变更绑定检测员");
        }
    }

    private void validateProjectGroup(DetectionProjectGroup entity, Long selfId) {
        validateEnabledFlag(entity.getEnabled(), "检测项目组启用状态不合法");
        ensureProjectGroupNameUnique(entity.getGroupName(), selfId);
    }

    private void validateParameter(DetectionParameter entity, Long selfId) {
        validateEnabledFlag(entity.getEnabled(), "检测参数启用状态不合法");
        ensureParameterNameUnique(entity.getParameterName(), selfId);
        BigDecimal min = entity.getStandardMin();
        BigDecimal max = entity.getStandardMax();
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessException("检测参数标准下限不能大于上限");
        }
        if (!isEnabled(entity.getEnabled()) && isParameterReferencedByEnabledType(selfId == null ? entity.getId() : selfId)) {
            throw new BusinessException("当前检测参数已被启用中的检测套餐引用，不能直接停用");
        }
    }

    private void validateMethod(DetectionMethod entity, Long selfId) {
        validateEnabledFlag(entity.getEnabled(), "检测方法启用状态不合法");
        ensureMethodNameUnique(entity.getMethodName(), selfId);
        ensureMethodCodeUnique(entity.getMethodCode(), selfId);
        if (!isEnabled(entity.getEnabled()) && isMethodReferencedByEnabledType(selfId == null ? entity.getId() : selfId)) {
            throw new BusinessException("当前检测方法已被启用中的检测套餐引用，不能直接停用");
        }
    }

    private void validateStep(DetectionStep entity, Long selfId) {
        DetectionType type = requireType(entity.getTypeId());
        entity.setTypeName(type.getTypeName());
        if (entity.getStepOrder() == null || entity.getStepOrder() <= 0) {
            throw new BusinessException("检测步骤顺序必须为正整数");
        }
        Long duplicateOrderCount = detectionStepMapper.selectCount(new LambdaQueryWrapper<DetectionStep>()
                .eq(DetectionStep::getTypeId, entity.getTypeId())
                .eq(DetectionStep::getStepOrder, entity.getStepOrder())
                .ne(selfId != null, DetectionStep::getId, selfId));
        if (duplicateOrderCount != null && duplicateOrderCount > 0) {
            throw new BusinessException("同一检测套餐下步骤顺序不能重复");
        }
        Long duplicateNameCount = detectionStepMapper.selectCount(new LambdaQueryWrapper<DetectionStep>()
                .eq(DetectionStep::getTypeId, entity.getTypeId())
                .eq(DetectionStep::getStepName, entity.getStepName())
                .ne(selfId != null, DetectionStep::getId, selfId));
        if (duplicateNameCount != null && duplicateNameCount > 0) {
            throw new BusinessException("同一检测套餐下步骤名称不能重复");
        }
    }

    private void ensureTypeNameUnique(String typeName, Long selfId) {
        Long duplicateCount = detectionTypeMapper.selectCount(new LambdaQueryWrapper<DetectionType>()
                .eq(DetectionType::getTypeName, typeName)
                .ne(selfId != null, DetectionType::getId, selfId));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new BusinessException("检测套餐名称不能重复");
        }
    }

    private void ensureProjectGroupNameUnique(String groupName, Long selfId) {
        Long duplicateCount = detectionProjectGroupMapper.selectCount(new LambdaQueryWrapper<DetectionProjectGroup>()
                .eq(DetectionProjectGroup::getGroupName, groupName)
                .ne(selfId != null, DetectionProjectGroup::getId, selfId));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new BusinessException("检测项目组名称不能重复");
        }
    }

    private void ensureParameterNameUnique(String parameterName, Long selfId) {
        Long duplicateCount = detectionParameterMapper.selectCount(new LambdaQueryWrapper<DetectionParameter>()
                .eq(DetectionParameter::getParameterName, parameterName)
                .ne(selfId != null, DetectionParameter::getId, selfId));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new BusinessException("检测参数名称不能重复");
        }
    }

    private void ensureMethodNameUnique(String methodName, Long selfId) {
        Long duplicateCount = detectionMethodMapper.selectCount(new LambdaQueryWrapper<DetectionMethod>()
                .eq(DetectionMethod::getMethodName, methodName)
                .ne(selfId != null, DetectionMethod::getId, selfId));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new BusinessException("检测方法名称不能重复");
        }
    }

    private void ensureMethodCodeUnique(String methodCode, Long selfId) {
        if (StrUtil.isBlank(methodCode)) {
            return;
        }
        Long duplicateCount = detectionMethodMapper.selectCount(new LambdaQueryWrapper<DetectionMethod>()
                .eq(DetectionMethod::getMethodCode, methodCode)
                .ne(selfId != null, DetectionMethod::getId, selfId));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new BusinessException("检测方法编码不能重复");
        }
    }

    private List<DetectionParameter> resolveTypeParameters(String parameterIdsText) {
        List<Long> parameterIds = parseParameterIds(parameterIdsText);
        if (parameterIds.isEmpty()) {
            return new ArrayList<>();
        }
        return resolveParametersByIds(parameterIds);
    }

    private List<DetectionParameter> resolveParametersByIds(List<Long> parameterIds) {
        if (parameterIds == null || parameterIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<DetectionParameter> parameters = detectionParameterMapper.selectList(new LambdaQueryWrapper<DetectionParameter>()
                .in(DetectionParameter::getId, parameterIds));
        Map<Long, DetectionParameter> parameterMap = parameters.stream()
                .collect(Collectors.toMap(DetectionParameter::getId, parameter -> parameter));
        List<DetectionParameter> orderedParameters = new ArrayList<>();
        for (Long parameterId : parameterIds) {
            DetectionParameter parameter = parameterMap.get(parameterId);
            if (parameter == null) {
                throw new BusinessException("检测套餐绑定了不存在的检测参数：" + parameterId);
            }
            orderedParameters.add(parameter);
        }
        return orderedParameters;
    }

    private List<Long> parseParameterIds(String parameterIdsText) {
        if (StrUtil.isBlank(parameterIdsText)) {
            return new ArrayList<>();
        }
        Map<Long, Boolean> orderedIds = new LinkedHashMap<>();
        for (String rawId : StrUtil.split(parameterIdsText, ',')) {
            String idText = StrUtil.trim(rawId);
            if (StrUtil.isBlank(idText)) {
                continue;
            }
            Long parameterId;
            try {
                parameterId = Long.valueOf(idText);
            } catch (NumberFormatException ex) {
                throw new BusinessException("检测套餐参数ID格式不合法：" + idText);
            }
            if (orderedIds.put(parameterId, Boolean.TRUE) != null) {
                throw new BusinessException("检测套餐参数不能重复配置：" + parameterId);
            }
        }
        return new ArrayList<>(orderedIds.keySet());
    }

    private String normalizeIdList(String parameterIdsText) {
        return parseParameterIds(parameterIdsText).stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<Long> normalizeMethodIds(List<Long> methodIds) {
        if (methodIds == null || methodIds.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(new LinkedHashSet<>(methodIds.stream()
                .filter(id -> id != null)
                .collect(Collectors.toList())));
    }

    private Map<Long, DetectionMethod> resolveSelectedMethods(List<Long> methodIds) {
        if (methodIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DetectionMethod> methods = detectionMethodMapper.selectList(new LambdaQueryWrapper<DetectionMethod>()
                .in(DetectionMethod::getId, methodIds));
        Map<Long, DetectionMethod> methodMap = methods.stream()
                .collect(Collectors.toMap(DetectionMethod::getId, method -> method));
        for (Long methodId : methodIds) {
            if (!methodMap.containsKey(methodId)) {
                throw new BusinessException("检测方法不存在：" + methodId);
            }
        }
        return methodMap;
    }

    private List<DetectionTypeParameterMethodBindingItem> parseTypeParameterMethodBindings(String jsonText) {
        if (StrUtil.isBlank(jsonText)) {
            return new ArrayList<>();
        }
        try {
            List<DetectionTypeParameterMethodBindingItem> items = objectMapper.readValue(
                    jsonText,
                    new TypeReference<List<DetectionTypeParameterMethodBindingItem>>() {
                    }
            );
            return items == null ? new ArrayList<>() : items;
        } catch (JsonProcessingException ex) {
            throw new BusinessException("检测套餐参数方法绑定关系格式不正确");
        }
    }

    private String serializeTypeParameterMethodBindings(List<DetectionTypeParameterMethodBindingItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("检测套餐参数方法绑定关系序列化失败");
        }
    }

    private void clearMethodBindingsByParameter(Long parameterId) {
        detectionMethodMapper.update(
                null,
                new LambdaUpdateWrapper<DetectionMethod>()
                        .eq(DetectionMethod::getParameterId, parameterId)
                        .set(DetectionMethod::getParameterId, null)
                        .set(DetectionMethod::getParameterName, null)
        );
    }

    private void validateAndPopulateTypeParameters(DetectionType entity) {
        List<DetectionTypeParameterMethodBindingItem> bindingItems = parseTypeParameterMethodBindings(entity.getParameterMethodBindings());
        if (!bindingItems.isEmpty()) {
            populateTypeFromParameterMethodBindings(entity, bindingItems);
            return;
        }

        List<DetectionParameter> parameters = resolveTypeParameters(entity.getParameterIds());
        if (parameters.isEmpty()) {
            throw new BusinessException("检测套餐至少要绑定一个检测参数");
        }
        List<Long> disabledParameterIds = parameters.stream()
                .filter(parameter -> !isEnabled(parameter.getEnabled()))
                .map(DetectionParameter::getId)
                .collect(Collectors.toList());
        if (!disabledParameterIds.isEmpty()) {
            throw new BusinessException("检测套餐不能绑定已停用的检测参数");
        }
        entity.setParameterNames(parameters.stream()
                .map(DetectionParameter::getParameterName)
                .collect(Collectors.joining(",")));
    }

    private void populateTypeFromParameterMethodBindings(DetectionType entity,
                                                         List<DetectionTypeParameterMethodBindingItem> bindingItems) {
        List<Long> orderedParameterIds = new ArrayList<>();
        Map<Long, List<Long>> bindingMethodIds = new LinkedHashMap<>();
        for (DetectionTypeParameterMethodBindingItem item : bindingItems) {
            if (item == null || item.getParameterId() == null) {
                throw new BusinessException("检测套餐存在未指定检测参数的绑定项");
            }
            if (bindingMethodIds.containsKey(item.getParameterId())) {
                throw new BusinessException("检测套餐中同一个检测参数不能重复配置");
            }
            List<Long> methodIds = normalizeMethodIds(item.getMethodIds());
            if (methodIds.isEmpty()) {
                throw new BusinessException("检测参数至少需要绑定一个检测方法");
            }
            if (methodIds.size() > 1) {
                throw new BusinessException("检测套餐中每个检测参数只能选择一个检测方法");
            }
            orderedParameterIds.add(item.getParameterId());
            bindingMethodIds.put(item.getParameterId(), methodIds);
        }

        List<DetectionParameter> parameters = resolveParametersByIds(orderedParameterIds);
        List<Long> disabledParameterIds = parameters.stream()
                .filter(parameter -> !isEnabled(parameter.getEnabled()))
                .map(DetectionParameter::getId)
                .collect(Collectors.toList());
        if (!disabledParameterIds.isEmpty()) {
            throw new BusinessException("检测套餐不能绑定已停用的检测参数");
        }
        Map<Long, DetectionParameter> parameterMap = parameters.stream()
                .collect(Collectors.toMap(DetectionParameter::getId, parameter -> parameter));

        List<Long> allMethodIds = bindingMethodIds.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        Map<Long, DetectionMethod> methodMap = resolveSelectedMethods(allMethodIds);
        List<Long> disabledMethodIds = methodMap.values().stream()
                .filter(method -> !isEnabled(method.getEnabled()))
                .map(DetectionMethod::getId)
                .collect(Collectors.toList());
        if (!disabledMethodIds.isEmpty()) {
            throw new BusinessException("当前检测套餐不能绑定已停用的检测方法");
        }

        LinkedHashSet<Long> duplicateMethodIds = new LinkedHashSet<>();
        LinkedHashSet<Long> visitedMethodIds = new LinkedHashSet<>();
        for (Long methodId : allMethodIds) {
            if (!visitedMethodIds.add(methodId)) {
                duplicateMethodIds.add(methodId);
            }
        }
        if (!duplicateMethodIds.isEmpty()) {
            throw new BusinessException("检测套餐中同一个检测方法不能重复配置");
        }

        List<DetectionTypeParameterMethodBindingItem> normalizedItems = new ArrayList<>();
        List<String> parameterNames = new ArrayList<>();
        List<String> parameterMethodNames = new ArrayList<>();
        for (Long parameterId : orderedParameterIds) {
            DetectionParameter parameter = parameterMap.get(parameterId);
            List<Long> methodIds = bindingMethodIds.get(parameterId);
            List<String> methodNames = new ArrayList<>();
            for (Long methodId : methodIds) {
                DetectionMethod method = methodMap.get(methodId);
                if (method == null) {
                    throw new BusinessException("检测方法不存在：" + methodId);
                }
                if (!parameterId.equals(method.getParameterId())) {
                    throw new BusinessException("检测方法“" + method.getMethodName() + "”未绑定到检测参数“"
                            + parameter.getParameterName() + "”，不能加入当前检测套餐");
                }
                methodNames.add(method.getMethodName());
            }

            DetectionTypeParameterMethodBindingItem normalizedItem = new DetectionTypeParameterMethodBindingItem();
            normalizedItem.setParameterId(parameterId);
            normalizedItem.setParameterName(parameter.getParameterName());
            normalizedItem.setMethodIds(new ArrayList<>(methodIds));
            normalizedItem.setMethodNames(methodNames);
            normalizedItems.add(normalizedItem);

            parameterNames.add(parameter.getParameterName());
            parameterMethodNames.add(parameter.getParameterName() + "（" + String.join("、", methodNames) + "）");
        }

        entity.setParameterIds(orderedParameterIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        entity.setParameterNames(String.join(",", parameterNames));
        entity.setParameterMethodBindings(serializeTypeParameterMethodBindings(normalizedItems));
        entity.setParameterMethodNames(String.join("；", parameterMethodNames));
    }

    private void applyProjectGroupToType(DetectionType entity, Long groupId) {
        if (groupId == null) {
            entity.setGroupId(null);
            entity.setGroupName(null);
            return;
        }
        DetectionProjectGroup group = requireProjectGroup(groupId);
        if (!isEnabled(group.getEnabled())) {
            throw new BusinessException("当前检测项目组已停用，不能绑定");
        }
        entity.setGroupId(group.getId());
        entity.setGroupName(group.getGroupName());
    }

    private void applyDetectorToType(DetectionType entity, Long detectorId) {
        if (detectorId == null) {
            entity.setDetectorId(null);
            entity.setDetectorName(null);
            return;
        }
        LabUser detector = requireDetector(detectorId);
        entity.setDetectorId(detector.getId());
        entity.setDetectorName(StrUtil.blankToDefault(StrUtil.trim(detector.getRealName()), detector.getUsername()));
    }

    private LabUser requireDetector(Long detectorId) {
        LabUser detector = labUserMapper.selectById(detectorId);
        if (detector == null) {
            throw new BusinessException("绑定的检测员不存在");
        }
        if (!Integer.valueOf(1).equals(detector.getStatus())) {
            throw new BusinessException("绑定的检测员已停用");
        }
        if (!StrUtil.equalsIgnoreCase(DETECTOR_ROLE_CODE, detector.getRoleCode())) {
            throw new BusinessException("绑定用户不是检测员角色，不能用于检测套餐");
        }
        return detector;
    }

    private void syncProjectGroupProjects(DetectionProjectGroup group, List<Long> projectIds) {
        clearProjectGroupBindings(group.getId());
        List<Long> normalizedProjectIds = projectIds == null ? Collections.emptyList() : projectIds.stream()
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        for (Long projectId : normalizedProjectIds) {
            DetectionType project = requireType(projectId);
            project.setGroupId(group.getId());
            project.setGroupName(group.getGroupName());
            detectionTypeMapper.updateById(project);
        }
    }

    private void clearProjectGroupBindings(Long groupId) {
        detectionTypeMapper.selectList(new LambdaQueryWrapper<DetectionType>()
                        .eq(DetectionType::getGroupId, groupId))
                .forEach(project -> {
                    project.setGroupId(null);
                    project.setGroupName(null);
                    detectionTypeMapper.updateById(project);
                });
    }

    private void validateEnabledFlag(Integer enabled, String message) {
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            throw new BusinessException(message);
        }
    }

    private boolean isEnabled(Integer enabled) {
        return Integer.valueOf(1).equals(enabled);
    }

    private boolean isParameterReferencedByType(Long parameterId) {
        if (parameterId == null) {
            return false;
        }
        Long typeCount = detectionTypeMapper.selectCount(new LambdaQueryWrapper<DetectionType>()
                .like(DetectionType::getParameterIds, String.valueOf(parameterId)));
        if (typeCount == null || typeCount == 0) {
            return false;
        }
        return detectionTypeMapper.selectList(new LambdaQueryWrapper<DetectionType>()
                .like(DetectionType::getParameterIds, String.valueOf(parameterId))).stream()
                .anyMatch(type -> parseParameterIds(type.getParameterIds()).contains(parameterId));
    }

    private boolean isParameterReferencedByEnabledType(Long parameterId) {
        if (parameterId == null) {
            return false;
        }
        return detectionTypeMapper.selectList(new LambdaQueryWrapper<DetectionType>()
                        .eq(DetectionType::getEnabled, 1)
                        .like(DetectionType::getParameterIds, String.valueOf(parameterId)))
                .stream()
                .anyMatch(type -> parseParameterIds(type.getParameterIds()).contains(parameterId));
    }

    private boolean isMethodReferencedByType(Long methodId) {
        if (methodId == null) {
            return false;
        }
        return detectionTypeMapper.selectList(new LambdaQueryWrapper<DetectionType>()
                        .like(DetectionType::getParameterMethodBindings, "\"" + methodId + "\""))
                .stream()
                .anyMatch(type -> containsMethodId(type.getParameterMethodBindings(), methodId));
    }

    private boolean isMethodReferencedByEnabledType(Long methodId) {
        if (methodId == null) {
            return false;
        }
        return detectionTypeMapper.selectList(new LambdaQueryWrapper<DetectionType>()
                        .eq(DetectionType::getEnabled, 1)
                        .like(DetectionType::getParameterMethodBindings, "\"" + methodId + "\""))
                .stream()
                .anyMatch(type -> containsMethodId(type.getParameterMethodBindings(), methodId));
    }

    private boolean containsMethodId(String bindingJson, Long methodId) {
        return parseTypeParameterMethodBindings(bindingJson).stream()
                .anyMatch(item -> normalizeMethodIds(item.getMethodIds()).contains(methodId));
    }

    private void syncStepTypeName(DetectionType type) {
        detectionStepMapper.selectList(new LambdaQueryWrapper<DetectionStep>()
                        .eq(DetectionStep::getTypeId, type.getId()))
                .forEach(step -> {
                    step.setTypeName(type.getTypeName());
                    detectionStepMapper.updateById(step);
                });
    }

    private void syncTypeParameterSnapshots(Long parameterId) {
        if (parameterId == null) {
            return;
        }
        detectionTypeMapper.selectList(new LambdaQueryWrapper<DetectionType>()
                        .like(DetectionType::getParameterIds, String.valueOf(parameterId)))
                .forEach(type -> {
                    validateAndPopulateTypeParameters(type);
                    detectionTypeMapper.updateById(type);
                });
    }

    private void syncTypeMethodBindingsByMethodId(Long methodId) {
        if (methodId == null) {
            return;
        }
        detectionTypeMapper.selectList(new LambdaQueryWrapper<DetectionType>()
                        .like(DetectionType::getParameterMethodBindings, "\"" + methodId + "\""))
                .forEach(type -> {
                    if (containsMethodId(type.getParameterMethodBindings(), methodId)) {
                        validateAndPopulateTypeParameters(type);
                        detectionTypeMapper.updateById(type);
                    }
                });
    }

    private void syncMethodParameterSnapshots(Long parameterId, String parameterName) {
        if (parameterId == null) {
            return;
        }
        detectionMethodMapper.selectList(new LambdaQueryWrapper<DetectionMethod>()
                        .eq(DetectionMethod::getParameterId, parameterId))
                .forEach(method -> {
                    method.setParameterName(parameterName);
                    detectionMethodMapper.updateById(method);
                });
    }

    private void ensureRemovedMethodsNotReferenced(Long parameterId, List<Long> nextMethodIds) {
        List<Long> currentMethodIds = detectionMethodMapper.selectList(new LambdaQueryWrapper<DetectionMethod>()
                        .eq(DetectionMethod::getParameterId, parameterId))
                .stream()
                .map(DetectionMethod::getId)
                .collect(Collectors.toList());
        for (Long currentMethodId : currentMethodIds) {
            if (!nextMethodIds.contains(currentMethodId) && isMethodReferencedByType(currentMethodId)) {
                DetectionMethod method = detectionMethodMapper.selectById(currentMethodId);
                String methodName = method == null ? String.valueOf(currentMethodId) : method.getMethodName();
                throw new BusinessException("检测方法“" + methodName + "”已被检测套餐引用，请先调整检测套餐后再解除绑定");
            }
        }
    }

    private boolean hasProcessingDetections(Long typeId) {
        Long count = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionTypeId, typeId)
                .eq(DetectionRecord::getDetectionStatus, SUBMITTED_DETECTION_STATUS));
        return count != null && count > 0L;
    }

    private boolean equalsNullableLong(Long left, Long right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private DetectionDetectorOptionVO toDetectorOption(LabUser entity) {
        DetectionDetectorOptionVO vo = new DetectionDetectorOptionVO();
        vo.setUserId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setRealName(entity.getRealName());
        vo.setRoleCode(entity.getRoleCode());
        vo.setDisplayName(StrUtil.blankToDefault(StrUtil.trim(entity.getRealName()), entity.getUsername())
                + "（" + entity.getUsername() + "）");
        return vo;
    }

    private void fillTypeParameterMethodNames(List<DetectionType> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (DetectionType record : records) {
            List<DetectionTypeParameterMethodBindingItem> items = parseTypeParameterMethodBindings(record.getParameterMethodBindings());
            if (items.isEmpty()) {
                record.setParameterMethodNames(null);
                continue;
            }
            List<String> segments = new ArrayList<>();
            for (DetectionTypeParameterMethodBindingItem item : items) {
                String parameterName = StrUtil.blankToDefault(item.getParameterName(), String.valueOf(item.getParameterId()));
                List<String> methodNames = item.getMethodNames() == null
                        ? Collections.emptyList()
                        : item.getMethodNames().stream()
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.toList());
                if (methodNames.isEmpty()) {
                    segments.add(parameterName);
                } else {
                    segments.add(parameterName + "（" + String.join("、", methodNames) + "）");
                }
            }
            record.setParameterMethodNames(String.join("；", segments));
        }
    }

    private DetectionParameterMethodBindingVO toParameterMethodBindingVO(DetectionParameter parameter, List<DetectionMethod> methods) {
        DetectionParameterMethodBindingVO vo = new DetectionParameterMethodBindingVO();
        vo.setId(parameter.getId());
        vo.setParameterName(parameter.getParameterName());
        vo.setStandardMin(parameter.getStandardMin());
        vo.setStandardMax(parameter.getStandardMax());
        vo.setUnit(parameter.getUnit());
        vo.setReferenceStandard(parameter.getReferenceStandard());
        vo.setExceedRule(parameter.getExceedRule());
        vo.setEnabled(parameter.getEnabled());
        vo.setRemark(parameter.getRemark());
        vo.setUpdatedTime(parameter.getUpdatedTime());

        List<DetectionMethod> bindingMethods = methods == null ? Collections.emptyList() : methods;
        vo.setMethodIds(bindingMethods.stream()
                .map(DetectionMethod::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        vo.setMethodNames(bindingMethods.stream()
                .map(DetectionMethod::getMethodName)
                .collect(Collectors.joining("、")));
        vo.setMethodCount(bindingMethods.size());
        return vo;
    }
}
