package com.yx.lab.modules.statistics.vo;

import lombok.Data;

/**
 * 统计时间跨度数据。
 */
@Data
public class StatisticsTimeBucketVO {

    private String label;

    private Long sampleTotal;

    private Long detectionTotal;

    private Long reviewTotal;

    private Long reportTotal;
}
