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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SamplingPlanService {

    private static final Set<String> DISPATCHABLE_PLAN_STATUSES =
            new HashSet<>(Arrays.asList("ACTIVE", "UNPUBLISHED"));

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
        return requirePlan(id);
    }

    public void save(SamplingPlan plan) {
        if (StrUtil.isBlank(plan.getPlanStatus())) {
            plan.setPlanStatus("ACTIVE");
        }
        if (StrUtil.isBlank(plan.getCycleType())) {
            plan.setCycleType("ONCE");
        }
        samplingPlanMapper.insert(plan);
    }

    public void update(SamplingPlan plan) {
        SamplingPlan existing = requirePlan(plan.getId());
        if (isLockedPlan(existing.getPlanStatus())) {
            throw new BusinessException("当前计划已进入执行阶段，不允许直接编辑");
        }
        if (StrUtil.isBlank(plan.getPlanStatus())) {
            plan.setPlanStatus(existing.getPlanStatus());
        }
        if (StrUtil.isBlank(plan.getCycleType())) {
            plan.setCycleType(existing.getCycleType());
        }
        samplingPlanMapper.updateById(plan);
    }

    public void delete(Long id) {
        SamplingPlan existing = requirePlan(id);
        if (isLockedPlan(existing.getPlanStatus())) {
            throw new BusinessException("当前计划已执行，不允许删除");
        }
        samplingPlanMapper.deleteById(id);
    }

    public void pause(Long id) {
        SamplingPlan plan = requirePlan(id);
        if (!"ACTIVE".equals(plan.getPlanStatus()) && !"UNPUBLISHED".equals(plan.getPlanStatus())) {
            throw new BusinessException("当前计划状态不允许暂停");
        }
        plan.setPlanStatus("PAUSED");
        samplingPlanMapper.updateById(plan);
    }

    public void resume(Long id) {
        SamplingPlan plan = requirePlan(id);
        if (!"PAUSED".equals(plan.getPlanStatus())) {
            throw new BusinessException("当前计划不处于暂停状态");
        }
        plan.setPlanStatus("ACTIVE");
        samplingPlanMapper.updateById(plan);
    }

    @Transactional(rollbackFor = Exception.class)
    public void dispatch(SamplingPlanDispatchCommand command) {
        SamplingPlan plan = requirePlan(command.getPlanId());
        if (!DISPATCHABLE_PLAN_STATUSES.contains(plan.getPlanStatus())) {
            throw new BusinessException("当前计划状态不允许派发");
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
        task.setRemark("由采样计划派发生成");
        samplingTaskMapper.insert(task);

        plan.setPlanStatus("DISPATCHED");
        plan.setUpdatedTime(LocalDateTime.now());
        samplingPlanMapper.updateById(plan);
    }

    private SamplingPlan requirePlan(Long id) {
        SamplingPlan plan = samplingPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("采样计划不存在");
        }
        return plan;
    }

    private boolean isLockedPlan(String planStatus) {
        return "DISPATCHED".equals(planStatus) || "COMPLETED".equals(planStatus);
    }
}
