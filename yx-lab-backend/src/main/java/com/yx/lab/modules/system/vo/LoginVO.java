package com.yx.lab.modules.system.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVO {

    private String token;

    private Long userId;

    private String username;

    private String realName;

    private Long orgId;

    private String orgName;

    private String roleCode;

    private String phone;

    private String avatarUrl;
}
