package com.yx.lab.modules.mobile.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MobileDetectionHistoryVO {

    private Long id;

    private Long sampleId;

    private String sampleNo;

    private Long detectionTypeId;

    private String detectionTypeName;

    private String detectionResult;

    private String detectionResultDesc;

    private String detectionStatus;

    private String detectionStatusDesc;

    private String abnormalRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime detectionTime;
}
