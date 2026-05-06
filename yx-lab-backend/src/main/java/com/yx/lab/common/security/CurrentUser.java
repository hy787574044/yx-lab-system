package com.yx.lab.common.security;

import lombok.Data;

import java.io.Serializable;

@Data
public class CurrentUser implements Serializable {

    private Long userId;

    private String username;

    private String realName;

    private String roleCode;
}
