package com.yx.lab.modules.statistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.review.entity.ReviewRecord;
import com.yx.lab.modules.review.mapper.ReviewRecordMapper;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.statistics.vo.StatisticsSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final LabSampleMapper labSampleMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    private final ReviewRecordMapper reviewRecordMapper;

    public StatisticsSummaryVO summary() {
        long sampleTotal = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>());
        long normalTotal = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, "NORMAL"));
        long abnormalTotal = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, "ABNORMAL"));
        long reviewTotal = reviewRecordMapper.selectCount(new LambdaQueryWrapper<ReviewRecord>());
        long approvedTotal = reviewRecordMapper.selectCount(new LambdaQueryWrapper<ReviewRecord>()
                .eq(ReviewRecord::getReviewResult, "APPROVED"));
        long rejectedTotal = reviewRecordMapper.selectCount(new LambdaQueryWrapper<ReviewRecord>()
                .eq(ReviewRecord::getReviewResult, "REJECTED"));

        StatisticsSummaryVO vo = new StatisticsSummaryVO();
        vo.setSampleTotal(sampleTotal);
        vo.setNormalTotal(normalTotal);
        vo.setAbnormalTotal(abnormalTotal);
        vo.setPassRate(rate(normalTotal, sampleTotal));
        vo.setReviewTotal(reviewTotal);
        vo.setApprovedTotal(approvedTotal);
        vo.setRejectedTotal(rejectedTotal);
        vo.setApprovalRate(rate(approvedTotal, reviewTotal));
        return vo;
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}
