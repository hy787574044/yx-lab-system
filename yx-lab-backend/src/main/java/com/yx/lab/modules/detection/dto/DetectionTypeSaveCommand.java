package com.yx.lab.modules.detection.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class DetectionTypeSaveCommand {

    @NotBlank(message = "检测类型名称不能为空")
    private String typeName;

    private Long groupId;

    private Long detectorId;

    private String parameterIds;

    private String parameterNames;

    /**
     * 检测套餐内“检测参数 -> 检测方法”层级绑定关系。
     */
    private java.util.List<DetectionTypeParameterMethodBindingItem> parameterMethodBindings;

    @NotNull(message = "启用状态不能为空")
    private Integer enabled;

    private String remark;
}
