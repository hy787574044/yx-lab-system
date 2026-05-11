package com.yx.lab.modules.detection.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class DetectionMethodSaveCommand {

    @NotBlank(message = "检测方法名称不能为空")
    private String methodName;

    private String methodCode;

    private String standardCode;

    private String methodBasis;

    private String applyScope;

    @NotNull(message = "启用状态不能为空")
    private Integer enabled;

    private String remark;
}
