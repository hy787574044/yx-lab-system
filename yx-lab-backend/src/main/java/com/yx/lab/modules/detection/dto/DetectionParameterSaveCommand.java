package com.yx.lab.modules.detection.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class DetectionParameterSaveCommand {

    @NotBlank(message = "检测参数名称不能为空")
    private String parameterName;

    private BigDecimal standardMin;

    private BigDecimal standardMax;

    private String unit;

    private String exceedRule;

    private String referenceStandard;

    @NotNull(message = "启用状态不能为空")
    private Integer enabled;

    private String remark;
}
