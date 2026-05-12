package com.yx.lab.modules.detection.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DetectionParameterMethodBindingVO {

    private Long id;

    private String parameterName;

    private BigDecimal standardMin;

    private BigDecimal standardMax;

    private String unit;

    private String referenceStandard;

    private String exceedRule;

    private Integer enabled;

    private String remark;

    private LocalDateTime updatedTime;

    private String methodIds;

    private String methodNames;

    private Integer methodCount;
}
