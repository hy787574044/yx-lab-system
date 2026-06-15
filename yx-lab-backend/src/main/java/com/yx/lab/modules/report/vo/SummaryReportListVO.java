package com.yx.lab.modules.report.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SummaryReportListVO {

    private String reportKey;

    private String summaryType;

    private String summaryTypeLabel;

    private String dailyReportType;

    private String dailyReportTypeLabel;

    private String reportName;

    private String regionName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate periodStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate periodEnd;

    private String periodLabel;

    private Integer pointCount;

    private Integer taskCount;

    private Integer sampleCount;

    private Integer detectedTaskCount;

    private Integer resultItemCount;

    private String reportStatus;

    private String reportStatusLabel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime latestSamplingTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime latestDetectionTime;
}
