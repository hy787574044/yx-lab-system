package com.yx.lab.modules.report.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.lab.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class SummaryReportQuery extends PageQuery {

    private String summaryType;

    private String dailyReportType;

    private String keyword;

    private String regionName;

    private String pointName;

    private String reportStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFrom;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateTo;
}
