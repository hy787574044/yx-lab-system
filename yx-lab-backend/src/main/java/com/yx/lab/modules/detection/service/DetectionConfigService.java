package com.yx.lab.modules.detection.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.detection.dto.DetectionParameterQuery;
import com.yx.lab.modules.detection.dto.DetectionStepQuery;
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

    public void saveType(DetectionType entity) {
        detectionTypeMapper.insert(entity);
    }

    public void updateType(DetectionType entity) {
        detectionTypeMapper.updateById(entity);
    }

    public void deleteType(Long id) {
        detectionTypeMapper.deleteById(id);
    }

    public void saveParameter(DetectionParameter entity) {
        detectionParameterMapper.insert(entity);
    }

    public void updateParameter(DetectionParameter entity) {
        detectionParameterMapper.updateById(entity);
    }

    public void deleteParameter(Long id) {
        detectionParameterMapper.deleteById(id);
    }

    public void saveStep(DetectionStep entity) {
        detectionStepMapper.insert(entity);
    }

    public void updateStep(DetectionStep entity) {
        detectionStepMapper.updateById(entity);
    }

    public void deleteStep(Long id) {
        detectionStepMapper.deleteById(id);
    }
}
