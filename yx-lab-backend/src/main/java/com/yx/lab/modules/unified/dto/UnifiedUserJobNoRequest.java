package com.yx.lab.modules.unified.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UnifiedUserJobNoRequest {

    @NotBlank(message = "用户工号不能为空")
    private String jobNo;
}
