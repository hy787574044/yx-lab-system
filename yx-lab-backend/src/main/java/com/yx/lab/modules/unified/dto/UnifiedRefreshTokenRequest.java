package com.yx.lab.modules.unified.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UnifiedRefreshTokenRequest {

    private String grantType;

    private String clientId;

    private String clientSecret;

    @NotBlank(message = "refreshToken cannot be blank")
    private String refreshToken;
}
