package com.yx.lab.modules.mobile.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MobileReviewTodoVO {

    private Long id;

    private Long sampleId;

    private String sampleNo;

    private Long detectionTypeId;

    private String detectionTypeName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime detectionTime;

    private Long detectorId;

    private String detectorName;

    private String detectionResult;

    private String abnormalRemark;

    private String detectionStatus;
}
