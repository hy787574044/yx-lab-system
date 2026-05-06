package com.yx.lab.modules.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("lab_report_template")
@EqualsAndHashCode(callSuper = true)
public class ReportTemplate extends BaseEntity {

    private String reportType;

    private String templateName;

    private Integer defaultTemplate;

    private String templateContent;

    private String remark;
}
