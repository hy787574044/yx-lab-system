package com.yx.lab.modules.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class EmbedLoginRequest {

    @NotBlank(message = "第三方令牌不能为空")
    private String token;

    private String userId;

    private String jobNo;

    private String username;

    private String channelType;
}
