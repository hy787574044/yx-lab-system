package com.yx.lab.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 流程节点保存命令。
 */
@Data
@Schema(description = "流程节点保存命令")
public class FlowNodeCommand {

    @NotBlank(message = "节点名称不能为空")
    @Size(max = 64, message = "节点名称长度不能超过64个字符")
    @Schema(description = "节点名称", required = true)
    private String nodeName;

    @NotBlank(message = "审批角色不能为空")
    @Size(max = 64, message = "审批角色长度不能超过64个字符")
    @Schema(description = "审批角色", required = true)
    private String roleName;

    @NotBlank(message = "审批角色编码不能为空")
    @Size(max = 64, message = "审批角色编码长度不能超过64个字符")
    @Schema(description = "审批角色编码", required = true)
    private String roleCode;

    @Schema(description = "指定人员ID")
    private Long assigneeId;

    @Size(max = 64, message = "指定人员长度不能超过64个字符")
    @Schema(description = "指定人员")
    private String assigneeName;

    @NotNull(message = "是否必审不能为空")
    @Schema(description = "是否必审", required = true)
    private Boolean required;

    @NotBlank(message = "驳回方式不能为空")
    @Size(max = 32, message = "驳回方式长度不能超过32个字符")
    @Schema(description = "驳回方式", required = true)
    private String rejectMode;
}
