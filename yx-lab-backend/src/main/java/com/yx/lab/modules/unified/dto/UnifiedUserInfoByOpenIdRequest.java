package com.yx.lab.modules.unified.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UnifiedUserInfoByOpenIdRequest {

    @NotBlank(message = "accessToken cannot be blank")
    private String accessToken;

    @NotBlank(message = "openId cannot be blank")
    private String openId;
}
