package com.yx.lab.modules.report.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ReportTemplateSaveCommand {

    @NotBlank(message = "报告类型不能为空")
    private String reportType;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotNull(message = "默认模板标识不能为空")
    private Integer defaultTemplate;

    @NotBlank(message = "模板内容不能为空")
    private String templateContent;

    private String remark;
}
