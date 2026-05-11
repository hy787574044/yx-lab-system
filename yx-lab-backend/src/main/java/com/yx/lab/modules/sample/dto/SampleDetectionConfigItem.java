package com.yx.lab.modules.sample.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 样品登录时选定的检测参数与检测方法快照项。
 */
@Data
public class SampleDetectionConfigItem {

    /**
     * 检测参数主键。
     */
    private Long parameterId;

    /**
     * 检测参数名称。
     */
    private String parameterName;

    /**
     * 单位。
     */
    private String unit;

    /**
     * 标准下限。
     */
    private BigDecimal standardMin;

    /**
     * 标准上限。
     */
    private BigDecimal standardMax;

    /**
     * 参考标准。
     */
    private String referenceStandard;

    /**
     * 选定的检测方法主键。
     */
    private Long methodId;

    /**
     * 选定的检测方法名称。
     */
    private String methodName;
}
