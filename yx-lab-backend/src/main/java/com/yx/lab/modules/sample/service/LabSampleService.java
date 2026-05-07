package com.yx.lab.modules.sample.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.sample.dto.LabSampleQuery;
import com.yx.lab.modules.sample.dto.SampleLoginCommand;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.sample.mapper.SamplingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class LabSampleService {

    private final LabSampleMapper labSampleMapper;

    private final SamplingTaskMapper samplingTaskMapper;

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
                        .orderByDesc(LabSample::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public LabSample detail(Long id) {
        return labSampleMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public LabSample loginSample(SampleLoginCommand command) {
        SamplingTask task = null;
        if (command.getTaskId() != null) {
            task = samplingTaskMapper.selectById(command.getTaskId());
            if (task == null) {
                throw new BusinessException("采样任务不存在");
            }
            if (!LabWorkflowConstants.SamplingTaskStatus.COMPLETED.equals(task.getTaskStatus())) {
                throw new BusinessException("采样任务未完成，不能进行样品登录");
            }
        }

        LabSample sample = new LabSample();
        sample.setSampleNo(generateSampleNo());
        sample.setTaskId(command.getTaskId());
        sample.setPointId(command.getPointId());
        sample.setPointName(command.getPointName());
        sample.setSampleType(command.getSampleType());
        sample.setDetectionItems(command.getDetectionItems());
        sample.setSamplingTime(command.getSamplingTime());
        sample.setSamplerId(command.getSamplerId());
        sample.setSamplerName(command.getSamplerName());
        sample.setWeather(command.getWeather());
        sample.setStorageCondition(command.getStorageCondition());
        sample.setSampleStatus(LabWorkflowConstants.SampleStatus.LOGGED);
        sample.setRemark(command.getRemark());
        labSampleMapper.insert(sample);

        if (task != null) {
            task.setTaskStatus(LabWorkflowConstants.SamplingTaskStatus.COMPLETED);
            samplingTaskMapper.updateById(task);
        }
        return sample;
    }

    public void updateStatus(Long sampleId, String status, String resultSummary) {
        LabSample sample = labSampleMapper.selectById(sampleId);
        if (sample == null) {
            throw new BusinessException("样品不存在");
        }
        sample.setSampleStatus(status);
        sample.setResultSummary(resultSummary);
        labSampleMapper.updateById(sample);
    }

    private String generateSampleNo() {
        String prefix = DateUtil.format(new Date(), "yyyyMM");
        Long count = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()
                .likeRight(LabSample::getSampleNo, prefix));
        long next = count == null ? 1L : count + 1L;
        return prefix + String.format("%04d", next);
    }
}
