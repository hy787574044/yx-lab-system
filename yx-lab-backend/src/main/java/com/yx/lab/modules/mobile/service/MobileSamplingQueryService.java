package com.yx.lab.modules.mobile.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.modules.mobile.vo.MobileSamplingTodoVO;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.sample.mapper.SamplingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MobileSamplingQueryService {

    private final SamplingTaskMapper samplingTaskMapper;

    private final LabSampleMapper labSampleMapper;

    public List<MobileSamplingTodoVO> samplingTodo() {
        CurrentUser currentUser = requireCurrentUser();
        List<SamplingTask> tasks = samplingTaskMapper.selectList(new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getSamplerId, currentUser.getUserId())
                .in(SamplingTask::getTaskStatus,
                        LabWorkflowConstants.SamplingTaskStatus.PENDING,
                        LabWorkflowConstants.SamplingTaskStatus.IN_PROGRESS,
                        LabWorkflowConstants.SamplingTaskStatus.COMPLETED)
                .orderByAsc(SamplingTask::getSamplingTime));
        if (tasks.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> taskIds = tasks.stream()
                .map(SamplingTask::getId)
                .collect(Collectors.toList());
        Map<Long, LabSample> sampleMap = labSampleMapper.selectList(new LambdaQueryWrapper<LabSample>()
                        .in(LabSample::getTaskId, taskIds))
                .stream()
                .filter(sample -> sample.getTaskId() != null)
                .collect(Collectors.toMap(LabSample::getTaskId, sample -> sample, (left, right) -> left, LinkedHashMap::new));
        return tasks.stream()
                .filter(task -> shouldShow(task, sampleMap.get(task.getId())))
                .map(task -> toSamplingTodoVO(task, sampleMap.get(task.getId())))
                .collect(Collectors.toList());
    }

    private boolean shouldShow(SamplingTask task, LabSample sample) {
        if (LabWorkflowConstants.TODO_TASK_STATUSES.contains(task.getTaskStatus())) {
            return true;
        }
        return LabWorkflowConstants.SamplingTaskStatus.COMPLETED.equals(task.getTaskStatus()) && sample == null;
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("当前登录信息已失效，请重新登录");
        }
        return currentUser;
    }

    private MobileSamplingTodoVO toSamplingTodoVO(SamplingTask task, LabSample sample) {
        MobileSamplingTodoVO vo = new MobileSamplingTodoVO();
        vo.setId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setPlanId(task.getPlanId());
        vo.setPointId(task.getPointId());
        vo.setPointName(task.getPointName());
        vo.setSamplingTime(task.getSamplingTime());
        vo.setSamplerId(task.getSamplerId());
        vo.setSamplerName(task.getSamplerName());
        vo.setSampleType(task.getSampleType());
        vo.setDetectionItems(task.getDetectionItems());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setTaskSealNo(task.getSealNo());
        vo.setSampleRegisterStatus(task.getSampleRegisterStatus());
        vo.setFinishedTime(task.getFinishedTime());
        vo.setRemark(task.getRemark());
        if (sample != null) {
            vo.setSampleId(sample.getId());
            vo.setSampleNo(sample.getSampleNo());
            vo.setSealNo(sample.getSealNo());
            vo.setSampleStatus(sample.getSampleStatus());
            vo.setSampleLogged(Boolean.TRUE);
        } else {
            vo.setSampleLogged(Boolean.FALSE);
        }
        return vo;
    }
}
