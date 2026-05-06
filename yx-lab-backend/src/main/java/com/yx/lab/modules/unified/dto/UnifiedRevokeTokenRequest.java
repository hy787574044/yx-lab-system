package com.yx.lab.modules.unified.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UnifiedRevokeTokenRequest {

    private String clientId;

    private String clientSecret;

    @NotBlank(message = "accessToken cannot be blank")
    private String accessToken;
}
