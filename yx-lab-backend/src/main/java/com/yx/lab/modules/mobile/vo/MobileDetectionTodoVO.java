package com.yx.lab.modules.mobile.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MobileDetectionTodoVO {

    private Long sampleId;

    private String sampleNo;

    private String sealNo;

    private String pointName;

    private String sampleType;

    private String detectionItems;

    private String sampleStatus;

    private String resultSummary;

    private String samplerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime samplingTime;

    private String traceLog;
}
