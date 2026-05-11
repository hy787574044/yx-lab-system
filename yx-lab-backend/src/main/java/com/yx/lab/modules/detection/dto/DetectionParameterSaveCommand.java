package com.yx.lab.modules.detection.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

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

    /**
     * 当前检测参数绑定的检测方法主键集合。
     */
    private List<Long> methodIds;
}
