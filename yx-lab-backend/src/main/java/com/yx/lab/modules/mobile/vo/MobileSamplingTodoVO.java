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

    private String address;

    private String latitude;

    private String longitude;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime samplingTime;

    private Long samplerId;

    private String samplerName;

    private String sampleType;

    private String sampleTypeDesc;

    private String detectionItems;

    private Long detectionTypeId;

    private String detectionTypeName;

    private String detectionConfigSnapshot;

    private String sampleTotalVolume;

    private String sampleBottleCount;

    private String taskStatus;

    private String taskStatusDesc;

    private String sampleRegisterStatus;

    private String sampleRegisterStatusDesc;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedTime;

    private String remark;

    private Long sampleId;

    private String sampleNo;

    private String sampleStatus;

    private String sampleStatusDesc;

    private Boolean sampleLogged;

    /**
     * 现场照片地址集合（逗号分隔的完整URL）
     */
    private String photoUrls;
}
