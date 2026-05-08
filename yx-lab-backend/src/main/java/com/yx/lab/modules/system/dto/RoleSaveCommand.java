package com.yx.lab.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 角色新增或编辑命令。
 */
@Data
@Schema(description = "角色新增或编辑命令")
public class RoleSaveCommand {

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 32, message = "角色编码长度不能超过32个字符")
    @Schema(description = "角色编码", required = true)
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过64个字符")
    @Schema(description = "角色名称", required = true)
    private String roleName;

    @Size(max = 64, message = "适用范围长度不能超过64个字符")
    @Schema(description = "适用范围")
    private String roleScope;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态，1 启用，0 停用", required = true)
    private Integer status;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Schema(description = "备注")
    private String remark;
}
