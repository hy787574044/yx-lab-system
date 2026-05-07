package com.yx.lab.modules.unified.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UnifiedRoleUserRequest {

    @NotBlank(message = "角色ID不能为空")
    private String roleId;
}
