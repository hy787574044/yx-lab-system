package com.yx.lab.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 用户新增或编辑命令。
 */
@Data
@Schema(description = "用户新增或编辑命令")
public class UserSaveCommand {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过64个字符")
    @Schema(description = "用户名", required = true)
    private String username;

    @Size(max = 128, message = "密码长度不能超过128个字符")
    @Schema(description = "登录密码。新增时必填，编辑时留空表示不修改密码")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过64个字符")
    @Schema(description = "姓名", required = true)
    private String realName;

    @NotNull(message = "所属机构不能为空")
    @Schema(description = "所属机构ID", required = true)
    private Long orgId;

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 64, message = "角色编码长度不能超过64个字符")
    @Schema(description = "角色编码", required = true)
    private String roleCode;

    @Size(max = 32, message = "手机号长度不能超过32个字符")
    @Schema(description = "手机号")
    private String phone;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态，1 启用，0 停用", required = true)
    private Integer status;
}
