package com.yx.lab.modules.report.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class SummaryReportPreviewRowVO {

    private String rowKey;

    private String timeBucket;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime samplingTime;

    private String samplingTimeLabel;

    private String pointName;

    private String sampleTypeLabel;

    private String sampleNo;

    private Integer sourceTaskCount;

    private Map<String, String> parameterValues = new LinkedHashMap<>();
}
