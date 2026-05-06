package com.yx.lab.modules.mobile.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.review.entity.ReviewRecord;
import com.yx.lab.modules.review.mapper.ReviewRecordMapper;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.mapper.SamplingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
public class MobileController {

    private final SamplingTaskMapper samplingTaskMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    private final ReviewRecordMapper reviewRecordMapper;

    @GetMapping("/sampling/todo")
    public ApiResponse<List<SamplingTask>> samplingTodo() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        List<SamplingTask> tasks = samplingTaskMapper.selectList(new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getSamplerId, currentUser.getUserId())
                .in(SamplingTask::getTaskStatus, "PENDING", "IN_PROGRESS")
                .orderByAsc(SamplingTask::getSamplingTime));
        return ApiResponse.success(tasks);
    }

    @GetMapping("/review/history")
    public ApiResponse<List<ReviewRecord>> reviewHistory() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        List<ReviewRecord> list = reviewRecordMapper.selectList(new LambdaQueryWrapper<ReviewRecord>()
                .eq(ReviewRecord::getReviewerId, currentUser.getUserId())
                .orderByDesc(ReviewRecord::getReviewTime));
        return ApiResponse.success(list);
    }

    @GetMapping("/review/todo")
    public ApiResponse<List<DetectionRecord>> reviewTodo() {
        List<DetectionRecord> list = detectionRecordMapper.selectList(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionStatus, "SUBMITTED")
                .orderByDesc(DetectionRecord::getDetectionTime));
        return ApiResponse.success(list);
    }
}
