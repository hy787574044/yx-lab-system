package com.yx.lab.modules.unified.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UnifiedAccessTokenRequest {

    private String grantType;

    private String clientId;

    private String clientSecret;

    @NotBlank(message = "授权码不能为空")
    private String code;
}
