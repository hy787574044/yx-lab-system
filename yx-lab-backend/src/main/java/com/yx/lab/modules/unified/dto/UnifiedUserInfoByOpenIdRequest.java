package com.yx.lab.modules.unified.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UnifiedUserInfoByOpenIdRequest {

    @NotBlank(message = "访问令牌不能为空")
    private String accessToken;

    @NotBlank(message = "OpenID 不能为空")
    private String openId;
}
