package com.yx.lab.modules.review.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.report.service.ReportService;
import com.yx.lab.modules.review.dto.ReviewCommand;
import com.yx.lab.modules.review.dto.ReviewQuery;
import com.yx.lab.modules.review.entity.ReviewRecord;
import com.yx.lab.modules.review.mapper.ReviewRecordMapper;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.sample.service.LabSampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRecordMapper reviewRecordMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    private final LabSampleMapper labSampleMapper;

    private final LabSampleService labSampleService;

    private final ReportService reportService;

    public PageResult<ReviewRecord> page(ReviewQuery query) {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        Page<ReviewRecord> page = reviewRecordMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<ReviewRecord>()
                        .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper.like(ReviewRecord::getSampleNo, query.getKeyword()))
                        .eq(StrUtil.isNotBlank(query.getReviewResult()), ReviewRecord::getReviewResult, query.getReviewResult())
                        .eq(Boolean.TRUE.equals(query.getMine()), ReviewRecord::getReviewerId, currentUser.getUserId())
                        .orderByDesc(ReviewRecord::getReviewTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    @Transactional(rollbackFor = Exception.class)
    public void review(ReviewCommand command) {
        DetectionRecord record = detectionRecordMapper.selectById(command.getDetectionRecordId());
        if (record == null) {
            throw new BusinessException("检测记录不存在");
        }
        if (!LabWorkflowConstants.canReviewDetection(record.getDetectionStatus())) {
            throw new BusinessException("当前检测记录不在待审核状态");
        }

        LabSample sample = labSampleMapper.selectById(record.getSampleId());
        if (sample == null) {
            throw new BusinessException("样品不存在");
        }

        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (LabWorkflowConstants.ReviewResult.REJECTED.equals(command.getReviewResult())
                && StrUtil.isBlank(command.getRejectReason())) {
            throw new BusinessException("驳回时必须填写驳回原因");
        }
        String rejectReason = StrUtil.trim(command.getRejectReason());
        String reviewRemark = StrUtil.trim(command.getReviewRemark());

        ReviewRecord reviewRecord = new ReviewRecord();
        reviewRecord.setDetectionRecordId(record.getId());
        reviewRecord.setSampleId(record.getSampleId());
        reviewRecord.setSampleNo(record.getSampleNo());
        reviewRecord.setReviewerId(currentUser.getUserId());
        reviewRecord.setReviewerName(currentUser.getRealName());
        reviewRecord.setReviewTime(LocalDateTime.now());
        reviewRecord.setReviewResult(command.getReviewResult());
        reviewRecord.setRejectReason(rejectReason);
        reviewRecord.setReviewRemark(reviewRemark);
        reviewRecordMapper.insert(reviewRecord);

        record.setDetectionStatus(LabWorkflowConstants.detectionStatusForReviewResult(command.getReviewResult()));
        detectionRecordMapper.updateById(record);

        String nextSampleStatus = LabWorkflowConstants.sampleStatusForReviewResult(command.getReviewResult());
        if (LabWorkflowConstants.ReviewResult.APPROVED.equals(command.getReviewResult())) {
            labSampleService.updateStatus(sample.getId(), nextSampleStatus, record.getDetectionResult());
            reportService.createApprovedReport(sample, record);
        } else {
            labSampleService.updateStatus(sample.getId(), nextSampleStatus, buildRetestSummary(rejectReason, reviewRemark));
        }
    }

    private String buildRetestSummary(String rejectReason, String reviewRemark) {
        if (StrUtil.isNotBlank(rejectReason)) {
            return "退回重检：" + rejectReason;
        }
        if (StrUtil.isNotBlank(reviewRemark)) {
            return "退回重检：" + reviewRemark;
        }
        return "退回重检";
    }
}
