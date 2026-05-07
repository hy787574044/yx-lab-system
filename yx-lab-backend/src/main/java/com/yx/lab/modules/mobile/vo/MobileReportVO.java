package com.yx.lab.modules.mobile.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MobileReportVO {

    private Long id;

    private Long sampleId;

    private String reportName;

    private String sampleNo;

    private String sealNo;

    private String reportStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedTime;

    private String publishedByName;

    private String pushStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastPushTime;

    private String lastPushMessage;
}
