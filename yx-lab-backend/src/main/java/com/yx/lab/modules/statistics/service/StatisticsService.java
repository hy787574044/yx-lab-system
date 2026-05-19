package com.yx.lab.modules.statistics.service;

import com.yx.lab.modules.statistics.vo.StatisticsSummaryVO;
import com.yx.lab.modules.statistics.vo.StatisticsTimeBucketVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 统计汇总服务，负责把基础计数组装成页面可直接展示的统计摘要。
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StatisticsQueryService statisticsQueryService;

    /**
     * 汇总统计概览数据。
     *
     * @return 统计摘要结果
     */
    public StatisticsSummaryVO summary() {
        long sampleTotal = statisticsQueryService.sampleTotal();
        long normalTotal = statisticsQueryService.normalTotal();
        long abnormalTotal = statisticsQueryService.abnormalTotal();
        long reviewTotal = statisticsQueryService.reviewTotal();
        long approvedTotal = statisticsQueryService.approvedReviewTotal();
        long rejectedTotal = statisticsQueryService.rejectedReviewTotal();

        StatisticsSummaryVO vo = new StatisticsSummaryVO();
        vo.setSampleTotal(sampleTotal);
        vo.setNormalTotal(normalTotal);
        vo.setAbnormalTotal(abnormalTotal);
        vo.setPassRate(rate(normalTotal, sampleTotal));
        vo.setReviewTotal(reviewTotal);
        vo.setApprovedTotal(approvedTotal);
        vo.setRejectedTotal(rejectedTotal);
        vo.setApprovalRate(rate(approvedTotal, reviewTotal));
        vo.setTimeBuckets(buildTimeBuckets());
        vo.setSampleTypeDistribution(statisticsQueryService.sampleTypeDistribution());
        vo.setSampleStatusDistribution(statisticsQueryService.sampleStatusDistribution());
        vo.setDetectionStatusDistribution(statisticsQueryService.detectionStatusDistribution());
        vo.setDetectionResultDistribution(statisticsQueryService.detectionResultDistribution());
        vo.setReviewResultDistribution(statisticsQueryService.reviewResultDistribution());
        vo.setReportStatusDistribution(statisticsQueryService.reportStatusDistribution());
        vo.setDetectionTypeRanking(statisticsQueryService.detectionTypeRanking());
        return vo;
    }

    private List<StatisticsTimeBucketVO> buildTimeBuckets() {
        LocalDate today = LocalDate.now();
        return Arrays.asList(
                buildTimeBucket("今日", today.atStartOfDay()),
                buildTimeBucket("近7日", today.minusDays(6).atStartOfDay()),
                buildTimeBucket("近30日", today.minusDays(29).atStartOfDay()),
                buildTimeBucket("全部", null));
    }

    private StatisticsTimeBucketVO buildTimeBucket(String label, LocalDateTime startTime) {
        StatisticsTimeBucketVO bucket = new StatisticsTimeBucketVO();
        bucket.setLabel(label);
        bucket.setSampleTotal(statisticsQueryService.sampleTotalFrom(startTime));
        bucket.setDetectionTotal(statisticsQueryService.detectionTotalFrom(startTime));
        bucket.setReviewTotal(statisticsQueryService.reviewTotalFrom(startTime));
        bucket.setReportTotal(statisticsQueryService.reportTotalFrom(startTime));
        return bucket;
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
