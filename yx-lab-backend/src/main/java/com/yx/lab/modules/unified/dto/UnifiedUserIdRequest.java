package com.yx.lab.modules.unified.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UnifiedUserIdRequest {

    @NotBlank(message = "用户ID不能为空")
    private String id;
}
