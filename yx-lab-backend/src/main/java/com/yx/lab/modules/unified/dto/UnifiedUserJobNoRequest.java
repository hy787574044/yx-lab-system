package com.yx.lab.modules.unified.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UnifiedUserJobNoRequest {

    @NotBlank(message = "jobNo cannot be blank")
    private String jobNo;
}
