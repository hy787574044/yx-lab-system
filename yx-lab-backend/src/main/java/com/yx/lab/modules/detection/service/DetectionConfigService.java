package com.yx.lab.modules.detection.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.detection.dto.DetectionParameterSaveCommand;
import com.yx.lab.modules.detection.dto.DetectionParameterQuery;
import com.yx.lab.modules.detection.dto.DetectionStepSaveCommand;
import com.yx.lab.modules.detection.dto.DetectionStepQuery;
import com.yx.lab.modules.detection.dto.DetectionTypeSaveCommand;
import com.yx.lab.modules.detection.dto.DetectionTypeQuery;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DetectionConfigService {

    private final DetectionTypeMapper detectionTypeMapper;

    private final DetectionParameterMapper detectionParameterMapper;

    private final DetectionStepMapper detectionStepMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    private final DetectionItemMapper detectionItemMapper;

    public PageResult<DetectionType> typePage(DetectionTypeQuery query) {
        Page<DetectionType> page = detectionTypeMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<DetectionType>()
                        .like(StrUtil.isNotBlank(query.getKeyword()), DetectionType::getTypeName, query.getKeyword())
                        .orderByDesc(DetectionType::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public PageResult<DetectionParameter> parameterPage(DetectionParameterQuery query) {
        Page<DetectionParameter> page = detectionParameterMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<DetectionParameter>()
                        .like(StrUtil.isNotBlank(query.getKeyword()), DetectionParameter::getParameterName, query.getKeyword())
                        .orderByDesc(DetectionParameter::getCreatedTime));
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

    public void saveType(DetectionTypeSaveCommand command) {
        DetectionType entity = new DetectionType();
        applyTypeCommand(entity, command);
        validateType(entity, null);
        detectionTypeMapper.insert(entity);
    }

    public void updateType(Long id, DetectionTypeSaveCommand command) {
        DetectionType entity = requireType(id);
        applyTypeCommand(entity, command);
        validateType(entity, id);
        detectionTypeMapper.updateById(entity);
        syncStepTypeName(entity);
    }

    public void deleteType(Long id) {
        DetectionType entity = requireType(id);
        Long stepCount = detectionStepMapper.selectCount(new LambdaQueryWrapper<DetectionStep>()
                .eq(DetectionStep::getTypeId, entity.getId()));
        if (stepCount != null && stepCount > 0) {
            throw new BusinessException("当前检测类型已配置检测步骤，不能删除");
        }
        Long recordCount = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionTypeId, entity.getId()));
        if (recordCount != null && recordCount > 0) {
            throw new BusinessException("当前检测类型已产生检测记录，不能删除");
        }
        detectionTypeMapper.deleteById(entity.getId());
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
        syncTypeParameterSnapshots(id, entity.getParameterName());
    }

    public void deleteParameter(Long id) {
        DetectionParameter entity = requireParameter(id);
        if (isParameterReferencedByType(entity.getId())) {
            throw new BusinessException("当前检测参数已被检测类型引用，不能删除");
        }
        Long itemCount = detectionItemMapper.selectCount(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getParameterId, entity.getId()));
        if (itemCount != null && itemCount > 0) {
            throw new BusinessException("当前检测参数已产生检测结果记录，不能删除");
        }
        detectionParameterMapper.deleteById(entity.getId());
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
            throw new BusinessException("检测类型不存在");
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

    private DetectionStep requireStep(Long id) {
        DetectionStep entity = detectionStepMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("检测步骤不存在");
        }
        return entity;
    }

    private void applyTypeCommand(DetectionType entity, DetectionTypeSaveCommand command) {
        entity.setTypeName(StrUtil.trim(command.getTypeName()));
        entity.setParameterIds(normalizeIdList(command.getParameterIds()));
        entity.setParameterNames(StrUtil.trim(command.getParameterNames()));
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

    private void validateType(DetectionType entity, Long selfId) {
        validateEnabledFlag(entity.getEnabled(), "检测类型启用状态不合法");
        ensureTypeNameUnique(entity.getTypeName(), selfId);
        List<DetectionParameter> parameters = resolveTypeParameters(entity.getParameterIds());
        if (parameters.isEmpty()) {
            throw new BusinessException("检测类型至少要绑定一个检测参数");
        }
        List<Long> disabledParameterIds = parameters.stream()
                .filter(parameter -> !isEnabled(parameter.getEnabled()))
                .map(DetectionParameter::getId)
                .collect(Collectors.toList());
        if (!disabledParameterIds.isEmpty()) {
            throw new BusinessException("检测类型不能绑定已停用的检测参数");
        }
        entity.setParameterNames(parameters.stream()
                .map(DetectionParameter::getParameterName)
                .collect(Collectors.joining(",")));
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
            throw new BusinessException("当前检测参数已被启用中的检测类型引用，不能直接停用");
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
            throw new BusinessException("同一检测类型下步骤顺序不能重复");
        }
        Long duplicateNameCount = detectionStepMapper.selectCount(new LambdaQueryWrapper<DetectionStep>()
                .eq(DetectionStep::getTypeId, entity.getTypeId())
                .eq(DetectionStep::getStepName, entity.getStepName())
                .ne(selfId != null, DetectionStep::getId, selfId));
        if (duplicateNameCount != null && duplicateNameCount > 0) {
            throw new BusinessException("同一检测类型下步骤名称不能重复");
        }
    }

    private void ensureTypeNameUnique(String typeName, Long selfId) {
        Long duplicateCount = detectionTypeMapper.selectCount(new LambdaQueryWrapper<DetectionType>()
                .eq(DetectionType::getTypeName, typeName)
                .ne(selfId != null, DetectionType::getId, selfId));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new BusinessException("检测类型名称不能重复");
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

    private List<DetectionParameter> resolveTypeParameters(String parameterIdsText) {
        List<Long> parameterIds = parseParameterIds(parameterIdsText);
        if (parameterIds.isEmpty()) {
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
                throw new BusinessException("检测类型绑定了不存在的检测参数：" + parameterId);
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
                throw new BusinessException("检测类型参数ID格式不合法：" + idText);
            }
            if (orderedIds.put(parameterId, Boolean.TRUE) != null) {
                throw new BusinessException("检测类型参数不能重复配置：" + parameterId);
            }
        }
        return new ArrayList<>(orderedIds.keySet());
    }

    private String normalizeIdList(String parameterIdsText) {
        return parseParameterIds(parameterIdsText).stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
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
                .like(DetectionType::getParameterIds, String.valueOf(parameterId))).stream()
                .anyMatch(type -> parseParameterIds(type.getParameterIds()).contains(parameterId));
    }

    private void syncStepTypeName(DetectionType type) {
        detectionStepMapper.selectList(new LambdaQueryWrapper<DetectionStep>()
                        .eq(DetectionStep::getTypeId, type.getId()))
                .forEach(step -> {
                    step.setTypeName(type.getTypeName());
                    detectionStepMapper.updateById(step);
                });
    }

    private void syncTypeParameterSnapshots(Long parameterId, String parameterName) {
        if (parameterId == null) {
            return;
        }
        detectionTypeMapper.selectList(new LambdaQueryWrapper<DetectionType>()
                        .like(DetectionType::getParameterIds, String.valueOf(parameterId)))
                .forEach(type -> {
                    List<DetectionParameter> parameters = resolveTypeParameters(type.getParameterIds());
                    type.setParameterNames(parameters.stream()
                            .map(DetectionParameter::getParameterName)
                            .collect(Collectors.joining(",")));
                    detectionTypeMapper.updateById(type);
                });
    }
}
