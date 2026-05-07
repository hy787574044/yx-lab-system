package com.yx.lab.modules.detection.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class DetectionStepSaveCommand {

    @NotNull(message = "检测类型不能为空")
    private Long typeId;

    @NotBlank(message = "检测类型名称不能为空")
    private String typeName;

    @NotBlank(message = "步骤名称不能为空")
    private String stepName;

    @NotNull(message = "步骤顺序不能为空")
    private Integer stepOrder;

    private String stepDescription;

    private String reagentRequirement;

    private String operationRequirement;

    private String remark;
}
