package com.yx.lab.modules.detection.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class DetectionTypeSaveCommand {

    @NotBlank(message = "检测类型名称不能为空")
    private String typeName;

    private String parameterIds;

    private String parameterNames;

    @NotNull(message = "启用状态不能为空")
    private Integer enabled;

    private String remark;
}
