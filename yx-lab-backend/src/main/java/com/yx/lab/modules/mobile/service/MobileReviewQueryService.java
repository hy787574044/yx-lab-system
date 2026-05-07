package com.yx.lab.modules.mobile.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.mobile.vo.MobileReviewHistoryVO;
import com.yx.lab.modules.mobile.vo.MobileReviewTodoVO;
import com.yx.lab.modules.review.entity.ReviewRecord;
import com.yx.lab.modules.review.mapper.ReviewRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MobileReviewQueryService {

    private final DetectionRecordMapper detectionRecordMapper;

    private final ReviewRecordMapper reviewRecordMapper;

    public List<MobileReviewHistoryVO> reviewHistory() {
        CurrentUser currentUser = requireCurrentUser();
        List<ReviewRecord> records = reviewRecordMapper.selectList(new LambdaQueryWrapper<ReviewRecord>()
                .eq(ReviewRecord::getReviewerId, currentUser.getUserId())
                .orderByDesc(ReviewRecord::getReviewTime));
        return records.stream()
                .map(this::toReviewHistoryVO)
                .collect(Collectors.toList());
    }

    public List<MobileReviewTodoVO> reviewTodo() {
        List<DetectionRecord> records = detectionRecordMapper.selectList(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionStatus, LabWorkflowConstants.DetectionStatus.SUBMITTED)
                .orderByDesc(DetectionRecord::getDetectionTime));
        return records.stream()
                .map(this::toReviewTodoVO)
                .collect(Collectors.toList());
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException("Please login first");
        }
        return currentUser;
    }

    private MobileReviewHistoryVO toReviewHistoryVO(ReviewRecord record) {
        MobileReviewHistoryVO vo = new MobileReviewHistoryVO();
        vo.setId(record.getId());
        vo.setDetectionRecordId(record.getDetectionRecordId());
        vo.setSampleId(record.getSampleId());
        vo.setSampleNo(record.getSampleNo());
        vo.setReviewerId(record.getReviewerId());
        vo.setReviewerName(record.getReviewerName());
        vo.setReviewTime(record.getReviewTime());
        vo.setReviewResult(record.getReviewResult());
        vo.setRejectReason(record.getRejectReason());
        vo.setReviewRemark(record.getReviewRemark());
        return vo;
    }

    private MobileReviewTodoVO toReviewTodoVO(DetectionRecord record) {
        MobileReviewTodoVO vo = new MobileReviewTodoVO();
        vo.setId(record.getId());
        vo.setSampleId(record.getSampleId());
        vo.setSampleNo(record.getSampleNo());
        vo.setDetectionTypeId(record.getDetectionTypeId());
        vo.setDetectionTypeName(record.getDetectionTypeName());
        vo.setDetectionTime(record.getDetectionTime());
        vo.setDetectorId(record.getDetectorId());
        vo.setDetectorName(record.getDetectorName());
        vo.setDetectionResult(record.getDetectionResult());
        vo.setAbnormalRemark(record.getAbnormalRemark());
        vo.setDetectionStatus(record.getDetectionStatus());
        return vo;
    }
}
