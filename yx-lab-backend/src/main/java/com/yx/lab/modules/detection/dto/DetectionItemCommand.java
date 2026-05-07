package com.yx.lab.modules.detection.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class DetectionItemCommand {

    @NotNull(message = "检测参数ID不能为空")
    private Long parameterId;

    @NotBlank(message = "检测参数不能为空")
    private String parameterName;

    private BigDecimal standardMin;

    private BigDecimal standardMax;

    @NotNull(message = "检测结果不能为空")
    private BigDecimal resultValue;

    private String unit;
}
