package com.yx.lab.common.security;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

@Data
public class CurrentUser implements Serializable {

    private Long userId;

    private String username;

    private String realName;

    private String roleCode;

    /**
     * 所属机构ID。
     */
    private Long orgId;

    /**
     * 所属机构名称。
     */
    private String orgName;

    /**
     * 当前登录角色拥有的权限码。
     */
    private Set<String> permissionCodes;

    /**
     * 数据范围：ALL 全部，SELF 本人相关，ORG 本机构。
     */
    private String dataScope;
}
