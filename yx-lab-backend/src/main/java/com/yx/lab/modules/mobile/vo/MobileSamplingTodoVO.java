package com.yx.lab.modules.mobile.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MobileSamplingTodoVO {

    private Long id;

    private String taskNo;

    private Long planId;

    private Long pointId;

    private String pointName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime samplingTime;

    private Long samplerId;

    private String samplerName;

    private String sampleType;

    private String detectionItems;

    private String taskStatus;

    private String taskSealNo;

    private String sampleRegisterStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedTime;

    private String remark;

    private Long sampleId;

    private String sampleNo;

    private String sealNo;

    private String sampleStatus;

    private Boolean sampleLogged;
}
