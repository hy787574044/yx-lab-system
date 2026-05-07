package com.yx.lab.modules.unified.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UnifiedRevokeTokenRequest {

    private String clientId;

    private String clientSecret;

    @NotBlank(message = "访问令牌不能为空")
    private String accessToken;
}
