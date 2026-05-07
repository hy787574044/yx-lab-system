package com.yx.lab.modules.sample.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
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
        CurrentUser currentUser = requireCurrentUser();
        SamplingTask task = null;
        if (command.getTaskId() != null) {
            task = samplingTaskMapper.selectById(command.getTaskId());
            if (task == null) {
                throw new BusinessException("采样任务不存在");
            }
            if (!LabWorkflowConstants.SamplingTaskStatus.COMPLETED.equals(task.getTaskStatus())) {
                throw new BusinessException("采样任务未完成，不能进行样品登录");
            }
            validateTaskOperator(task, currentUser);

            Long existingCount = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()
                    .eq(LabSample::getTaskId, task.getId()));
            if (existingCount != null && existingCount > 0) {
                throw new BusinessException("该采样任务已完成样品登录，不能重复登录");
            }
        } else {
            validateSamplerOperator(command, currentUser);
        }

        LabSample sample = new LabSample();
        sample.setSampleNo(generateSampleNo());
        sample.setTaskId(command.getTaskId());
        sample.setPointId(task == null || task.getPointId() == null ? command.getPointId() : task.getPointId());
        sample.setPointName(task == null || StrUtil.isBlank(task.getPointName()) ? command.getPointName() : task.getPointName());
        sample.setSampleType(task == null || StrUtil.isBlank(task.getSampleType()) ? command.getSampleType() : task.getSampleType());
        sample.setDetectionItems(command.getDetectionItems());
        sample.setSamplingTime(command.getSamplingTime());
        sample.setSamplerId(resolveSamplerId(command, task, currentUser));
        sample.setSamplerName(resolveSamplerName(command, task, currentUser));
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

    private CurrentUser requireCurrentUser() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("当前登录信息已失效，请重新登录");
        }
        return currentUser;
    }

    private void validateTaskOperator(SamplingTask task, CurrentUser currentUser) {
        if (isAdmin(currentUser)) {
            return;
        }
        if (task.getSamplerId() == null || !task.getSamplerId().equals(currentUser.getUserId())) {
            throw new BusinessException("当前用户不是该采样任务的责任采样员，不能进行样品登录");
        }
    }

    private void validateSamplerOperator(SampleLoginCommand command, CurrentUser currentUser) {
        if (isAdmin(currentUser)) {
            return;
        }
        if (!currentUser.getUserId().equals(command.getSamplerId())) {
            throw new BusinessException("当前用户只能登记本人采集的样品");
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
}
