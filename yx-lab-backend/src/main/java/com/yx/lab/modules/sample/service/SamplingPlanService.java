package com.yx.lab.modules.sample.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.sample.dto.SamplingPlanDispatchCommand;
import com.yx.lab.modules.sample.dto.SamplingPlanQuery;
import com.yx.lab.modules.sample.entity.SamplingPlan;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.mapper.SamplingPlanMapper;
import com.yx.lab.modules.sample.mapper.SamplingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SamplingPlanService {

    private final SamplingPlanMapper samplingPlanMapper;

    private final SamplingTaskMapper samplingTaskMapper;

    public PageResult<SamplingPlan> page(SamplingPlanQuery query) {
        Page<SamplingPlan> page = samplingPlanMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<SamplingPlan>()
                        .like(StrUtil.isNotBlank(query.getKeyword()), SamplingPlan::getPlanName, query.getKeyword())
                        .eq(StrUtil.isNotBlank(query.getPlanStatus()), SamplingPlan::getPlanStatus, query.getPlanStatus())
                        .orderByDesc(SamplingPlan::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public SamplingPlan detail(Long id) {
        return samplingPlanMapper.selectById(id);
    }

    public void save(SamplingPlan plan) {
        if (StrUtil.isBlank(plan.getPlanStatus())) {
            plan.setPlanStatus("UNPUBLISHED");
        }
        samplingPlanMapper.insert(plan);
    }

    public void update(SamplingPlan plan) {
        samplingPlanMapper.updateById(plan);
    }

    public void delete(Long id) {
        samplingPlanMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void dispatch(SamplingPlanDispatchCommand command) {
        SamplingPlan plan = samplingPlanMapper.selectById(command.getPlanId());
        if (plan == null) {
            throw new BusinessException("采样计划不存在");
        }
        SamplingTask task = new SamplingTask();
        task.setPlanId(plan.getId());
        task.setPointId(plan.getPointId());
        task.setPointName(plan.getPointName());
        task.setSamplingTime(command.getSamplingTime() == null ? plan.getStartTime() : command.getSamplingTime());
        task.setSamplerId(plan.getSamplerId());
        task.setSamplerName(plan.getSamplerName());
        task.setSampleType(plan.getSampleType());
        task.setTaskStatus("PENDING");
        task.setRemark("由采样计划派发");
        samplingTaskMapper.insert(task);

        plan.setPlanStatus("PUBLISHED");
        plan.setUpdatedTime(LocalDateTime.now());
        samplingPlanMapper.updateById(plan);
    }
}
