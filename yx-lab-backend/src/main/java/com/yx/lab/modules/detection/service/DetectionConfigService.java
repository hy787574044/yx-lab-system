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
import com.yx.lab.modules.detection.entity.DetectionParameter;
import com.yx.lab.modules.detection.entity.DetectionStep;
import com.yx.lab.modules.detection.entity.DetectionType;
import com.yx.lab.modules.detection.mapper.DetectionParameterMapper;
import com.yx.lab.modules.detection.mapper.DetectionStepMapper;
import com.yx.lab.modules.detection.mapper.DetectionTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionConfigService {

    private final DetectionTypeMapper detectionTypeMapper;

    private final DetectionParameterMapper detectionParameterMapper;

    private final DetectionStepMapper detectionStepMapper;

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
        detectionTypeMapper.insert(entity);
    }

    public void updateType(Long id, DetectionTypeSaveCommand command) {
        DetectionType entity = requireType(id);
        applyTypeCommand(entity, command);
        detectionTypeMapper.updateById(entity);
    }

    public void deleteType(Long id) {
        detectionTypeMapper.deleteById(requireType(id).getId());
    }

    public void saveParameter(DetectionParameterSaveCommand command) {
        DetectionParameter entity = new DetectionParameter();
        applyParameterCommand(entity, command);
        detectionParameterMapper.insert(entity);
    }

    public void updateParameter(Long id, DetectionParameterSaveCommand command) {
        DetectionParameter entity = requireParameter(id);
        applyParameterCommand(entity, command);
        detectionParameterMapper.updateById(entity);
    }

    public void deleteParameter(Long id) {
        detectionParameterMapper.deleteById(requireParameter(id).getId());
    }

    public void saveStep(DetectionStepSaveCommand command) {
        DetectionStep entity = new DetectionStep();
        applyStepCommand(entity, command);
        detectionStepMapper.insert(entity);
    }

    public void updateStep(Long id, DetectionStepSaveCommand command) {
        DetectionStep entity = requireStep(id);
        applyStepCommand(entity, command);
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
        entity.setParameterIds(StrUtil.trim(command.getParameterIds()));
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
}
