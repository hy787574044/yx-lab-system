package com.yx.lab.modules.mobile.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MobileSamplingTodoVO {

    private Long id;

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedTime;

    private String remark;
}
