package com.yx.lab.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程配置新增或编辑命令。
 */
@Data
@Schema(description = "流程配置新增或编辑命令")
public class FlowConfigSaveCommand {

    @NotBlank(message = "流程名称不能为空")
    @Size(max = 128, message = "流程名称长度不能超过128个字符")
    @Schema(description = "流程名称", required = true)
    private String flowName;

    @NotBlank(message = "流程类型不能为空")
    @Size(max = 32, message = "流程类型长度不能超过32个字符")
    @Schema(description = "流程类型", required = true)
    private String flowType;

    @NotBlank(message = "适用范围不能为空")
    @Size(max = 128, message = "适用范围长度不能超过128个字符")
    @Schema(description = "适用范围", required = true)
    private String scopeName;

    @NotNull(message = "默认流程不能为空")
    @Schema(description = "是否默认流程", required = true)
    private Boolean defaultFlag;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态，1 启用，0 停用", required = true)
    private Integer status;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Schema(description = "备注")
    private String remark;

    @Valid
    @Schema(description = "流程节点")
    private List<FlowNodeCommand> nodes = new ArrayList<>();
}
