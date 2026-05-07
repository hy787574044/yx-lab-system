package com.yx.lab.modules.unified.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UnifiedUserQueryRequest {

    @NotBlank(message = "用户工号不能为空")
    private String jobNo;

    @NotBlank(message = "手机号不能为空")
    private String mobile;
}
