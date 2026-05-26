package com.yx.lab.modules.mobile.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.modules.mobile.vo.MobileSamplingTodoVO;
import com.yx.lab.modules.sample.dto.SamplingTaskQuery;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.sample.mapper.SamplingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 移动端采样查询服务。
 * 采样待办复用 PC 端任务查询参数，并保持“只看当前采样员本人任务”的移动端权限约束。
 */
@Service
@RequiredArgsConstructor
public class MobileSamplingQueryService {

    private final SamplingTaskMapper samplingTaskMapper;

    private final LabSampleMapper labSampleMapper;

    /**
     * 分页查询当前采样员的移动端采样待办。
     *
     * @param query 采样任务分页查询条件
     * @return 移动端采样待办分页结果
     */
    public PageResult<MobileSamplingTodoVO> samplingTodo(SamplingTaskQuery query) {
        CurrentUser currentUser = requireCurrentUser();
        if (query != null && query.getSamplerId() != null && !query.getSamplerId().equals(currentUser.getUserId())) {
            return new PageResult<>(0L, Collections.emptyList());
        }
        String keyword = query == null ? null : StrUtil.trim(query.getKeyword());
        String taskStatus = query == null ? null : StrUtil.trim(query.getTaskStatus());

        List<SamplingTask> tasks = samplingTaskMapper.selectList(new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getSamplerId, currentUser.getUserId())
                .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                        .like(SamplingTask::getTaskNo, keyword)
                        .or()
                        .like(SamplingTask::getPointName, keyword)
                        .or()
                        .like(SamplingTask::getSealNo, keyword))
                .eq(StrUtil.isNotBlank(taskStatus), SamplingTask::getTaskStatus, taskStatus)
                .in(SamplingTask::getTaskStatus,
                        LabWorkflowConstants.SamplingTaskStatus.PENDING,
                        LabWorkflowConstants.SamplingTaskStatus.IN_PROGRESS,
                        LabWorkflowConstants.SamplingTaskStatus.COMPLETED)
                .orderByAsc(SamplingTask::getSamplingTime)
                .orderByDesc(SamplingTask::getCreatedTime));
        if (tasks.isEmpty()) {
            return new PageResult<>(0L, Collections.emptyList());
        }

        List<Long> taskIds = tasks.stream()
                .map(SamplingTask::getId)
                .collect(Collectors.toList());
        Map<Long, LabSample> sampleMap = labSampleMapper.selectList(new LambdaQueryWrapper<LabSample>()
                        .in(LabSample::getTaskId, taskIds))
                .stream()
                .filter(sample -> sample.getTaskId() != null)
                .collect(Collectors.toMap(LabSample::getTaskId, sample -> sample, (left, right) -> left, LinkedHashMap::new));

        List<MobileSamplingTodoVO> records = tasks.stream()
                .filter(task -> shouldShow(task, sampleMap.get(task.getId())))
                .map(task -> toSamplingTodoVO(task, sampleMap.get(task.getId())))
                .collect(Collectors.toList());
        return buildManualPageResult(records, query);
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
            throw new BusinessException("当前登录信息已失效，请重新登录。");
        }
        return currentUser;
    }

    private PageResult<MobileSamplingTodoVO> buildManualPageResult(List<MobileSamplingTodoVO> source,
                                                                   SamplingTaskQuery query) {
        long total = source == null ? 0L : source.size();
        if (total == 0L) {
            return new PageResult<>(0L, Collections.emptyList());
        }
        long pageNum = query == null || query.getPageNum() <= 0 ? 1L : query.getPageNum();
        long pageSize = query == null || query.getPageSize() <= 0 ? 10L : query.getPageSize();
        int fromIndex = (int) Math.min((pageNum - 1) * pageSize, total);
        int toIndex = (int) Math.min(fromIndex + pageSize, total);
        return new PageResult<>(total, new ArrayList<>(source.subList(fromIndex, toIndex)));
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
            // 如果没有样品，使用任务的封签号（支持提前录入封签号的场景）
            vo.setSealNo(task.getSealNo());
            vo.setSampleLogged(Boolean.FALSE);
        }
        return vo;
    }
}