package com.yx.lab.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@Schema(description = "登录请求")
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", required = true)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", required = true)
    private String password;

    @Schema(description = "验证码唯一标识，PC 登录必填")
    private String captchaId;

    @Schema(description = "验证码，PC 登录必填")
    private String captchaCode;
}
