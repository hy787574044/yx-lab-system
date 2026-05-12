package com.yx.lab.modules.report.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报告模板实体，维护报告预览与打印时使用的模板内容。
 */
@Data
@TableName("lab_report_template")
@EqualsAndHashCode(callSuper = true)
public class ReportTemplate extends BaseEntity {

    @Schema(description = "报告类型")
    @TableField("report_type")
    private String reportType;

    @Schema(description = "模板名称")
    @TableField("template_name")
    private String templateName;

    @Schema(description = "默认模板标记，1是，0否")
    @TableField("default_template")
    private Integer defaultTemplate;

    @Schema(description = "模板内容")
    @TableField("template_content")
    private String templateContent;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
