package com.yx.lab.modules.system.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class UserProfileVO {

    private Long userId;

    private String username;

    private String realName;

    private Long orgId;

    private String orgName;

    private String roleCode;

    private Set<String> permissionCodes;

    private String dataScope;

    private String phone;

    private String avatarUrl;

    private Integer status;
}
