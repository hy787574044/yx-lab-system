package com.yx.lab.modules.report.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SummaryReportPreviewVO {

    private String summaryType;

    private String summaryTypeLabel;

    private String dailyReportType;

    private String dailyReportTypeLabel;

    private String previewMode;

    private String reportName;

    private String regionName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate periodStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate periodEnd;

    private String periodLabel;

    private String weekdayLabel;

    private String weather;

    private String reportUnit;

    private String inspectorName;

    private String reporterName;

    private String principalName;

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

    private List<String> parameterColumns = new ArrayList<>();

    private List<SummaryReportPreviewRowVO> rows = new ArrayList<>();
}
