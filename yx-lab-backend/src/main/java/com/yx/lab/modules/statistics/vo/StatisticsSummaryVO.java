package com.yx.lab.modules.statistics.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StatisticsSummaryVO {

    private long sampleTotal;

    private long normalTotal;

    private long abnormalTotal;

    private BigDecimal passRate;

    private long reviewTotal;

    private long approvedTotal;

    private long rejectedTotal;

    private BigDecimal approvalRate;

    private List<StatisticsTimeBucketVO> timeBuckets;

    private List<StatisticsDimensionItemVO> sampleTypeDistribution;

    private List<StatisticsDimensionItemVO> sampleStatusDistribution;

    private List<StatisticsDimensionItemVO> detectionStatusDistribution;

    private List<StatisticsDimensionItemVO> detectionResultDistribution;

    private List<StatisticsDimensionItemVO> reviewResultDistribution;

    private List<StatisticsDimensionItemVO> reportStatusDistribution;

    private List<StatisticsDimensionItemVO> detectionTypeRanking;
}
