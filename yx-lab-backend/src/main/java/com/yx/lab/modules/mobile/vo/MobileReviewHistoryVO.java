package com.yx.lab.modules.mobile.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MobileReviewHistoryVO {

    private Long id;

    private Long detectionRecordId;

    private Long sampleId;

    private String sampleNo;

    private Long reviewerId;

    private String reviewerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;

    private String reviewResult;

    private String rejectReason;

    private String reviewRemark;
}
