package com.yx.lab.modules.report.dto;

import com.yx.lab.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReportQuery extends PageQuery {

    private String keyword;

    private String reportType;

    private String reportStatus;
}
