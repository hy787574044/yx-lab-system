package com.yx.lab.modules.sample.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.sample.dto.SamplingTaskCompleteCommand;
import com.yx.lab.modules.sample.dto.SamplingTaskQuery;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.mapper.SamplingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SamplingTaskService {

    private final SamplingTaskMapper samplingTaskMapper;

    public PageResult<SamplingTask> page(SamplingTaskQuery query) {
        Page<SamplingTask> page = samplingTaskMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<SamplingTask>()
                        .like(StrUtil.isNotBlank(query.getKeyword()), SamplingTask::getPointName, query.getKeyword())
                        .eq(StrUtil.isNotBlank(query.getTaskStatus()), SamplingTask::getTaskStatus, query.getTaskStatus())
                        .eq(query.getSamplerId() != null, SamplingTask::getSamplerId, query.getSamplerId())
                        .orderByDesc(SamplingTask::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public SamplingTask detail(Long id) {
        return samplingTaskMapper.selectById(id);
    }

    public List<SamplingTask> todoMine() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        return samplingTaskMapper.selectList(new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getSamplerId, currentUser.getUserId())
                .in(SamplingTask::getTaskStatus, "PENDING", "IN_PROGRESS")
                .orderByAsc(SamplingTask::getSamplingTime));
    }

    public void complete(SamplingTaskCompleteCommand command) {
        SamplingTask task = samplingTaskMapper.selectById(command.getTaskId());
        if (task == null) {
            throw new BusinessException("采样任务不存在");
        }
        task.setOnsiteMetrics(command.getOnsiteMetrics());
        task.setPhotoUrls(command.getPhotoUrls());
        task.setRemark(command.getRemark());
        task.setTaskStatus("COMPLETED");
        task.setFinishedTime(LocalDateTime.now());
        samplingTaskMapper.updateById(task);
    }
}
