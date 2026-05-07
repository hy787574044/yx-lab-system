package com.yx.lab.modules.unified.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UnifiedRefreshTokenRequest {

    private String grantType;

    private String clientId;

    private String clientSecret;

    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}
