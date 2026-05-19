package com.yx.lab.modules.statistics.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统计维度项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsDimensionItemVO {

    private String name;

    private Long value;
}
