package com.yx.lab.modules.mobile.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.modules.mobile.vo.MobileSamplingTodoVO;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.mapper.SamplingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MobileSamplingQueryService {

    private final SamplingTaskMapper samplingTaskMapper;

    public List<MobileSamplingTodoVO> samplingTodo() {
        CurrentUser currentUser = requireCurrentUser();
        List<SamplingTask> tasks = samplingTaskMapper.selectList(new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getSamplerId, currentUser.getUserId())
                .in(SamplingTask::getTaskStatus, LabWorkflowConstants.TODO_TASK_STATUSES)
                .orderByAsc(SamplingTask::getSamplingTime));
        return tasks.stream()
                .map(this::toSamplingTodoVO)
                .collect(Collectors.toList());
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException("Please login first");
        }
        return currentUser;
    }

    private MobileSamplingTodoVO toSamplingTodoVO(SamplingTask task) {
        MobileSamplingTodoVO vo = new MobileSamplingTodoVO();
        vo.setId(task.getId());
        vo.setPlanId(task.getPlanId());
        vo.setPointId(task.getPointId());
        vo.setPointName(task.getPointName());
        vo.setSamplingTime(task.getSamplingTime());
        vo.setSamplerId(task.getSamplerId());
        vo.setSamplerName(task.getSamplerName());
        vo.setSampleType(task.getSampleType());
        vo.setDetectionItems(task.getDetectionItems());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setFinishedTime(task.getFinishedTime());
        vo.setRemark(task.getRemark());
        return vo;
    }
}
