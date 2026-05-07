package com.yx.lab.modules.statistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.review.entity.ReviewRecord;
import com.yx.lab.modules.review.mapper.ReviewRecordMapper;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticsQueryService {

    private final LabSampleMapper labSampleMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    private final ReviewRecordMapper reviewRecordMapper;

    public long sampleTotal() {
        return labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>());
    }

    public long normalTotal() {
        return detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, LabWorkflowConstants.DetectionResult.NORMAL));
    }

    public long abnormalTotal() {
        return detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, LabWorkflowConstants.DetectionResult.ABNORMAL));
    }

    public long reviewTotal() {
        return reviewRecordMapper.selectCount(new LambdaQueryWrapper<ReviewRecord>());
    }

    public long approvedReviewTotal() {
        return reviewRecordMapper.selectCount(new LambdaQueryWrapper<ReviewRecord>()
                .eq(ReviewRecord::getReviewResult, LabWorkflowConstants.ReviewResult.APPROVED));
    }

    public long rejectedReviewTotal() {
        return reviewRecordMapper.selectCount(new LambdaQueryWrapper<ReviewRecord>()
                .eq(ReviewRecord::getReviewResult, LabWorkflowConstants.ReviewResult.REJECTED));
    }
}
