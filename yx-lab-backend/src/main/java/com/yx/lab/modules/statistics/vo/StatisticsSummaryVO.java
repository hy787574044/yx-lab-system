package com.yx.lab.modules.statistics.vo;

import lombok.Data;

import java.math.BigDecimal;

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
}
