package com.yx.lab.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 当前登录人密码修改命令。
 */
@Data
@Schema(description = "当前登录人密码修改命令")
public class PasswordChangeCommand {

    @NotBlank(message = "原密码不能为空")
    @Size(max = 128, message = "原密码长度不能超过128个字符")
    @Schema(description = "原密码", required = true)
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(max = 128, message = "新密码长度不能超过128个字符")
    @Schema(description = "新密码", required = true)
    private String newPassword;
}
