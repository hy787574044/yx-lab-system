package com.yx.lab.modules.unified.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UnifiedRoleUserRequest {

    @NotBlank(message = "roleId cannot be blank")
    private String roleId;
}
