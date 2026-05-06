package com.yx.lab.modules.unified.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UnifiedAccessTokenRequest {

    private String grantType;

    private String clientId;

    private String clientSecret;

    @NotBlank(message = "authorization code cannot be blank")
    private String code;
}
