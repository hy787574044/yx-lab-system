package com.yx.lab.modules.unified.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UnifiedUserIdRequest {

    @NotBlank(message = "id cannot be blank")
    private String id;
}
