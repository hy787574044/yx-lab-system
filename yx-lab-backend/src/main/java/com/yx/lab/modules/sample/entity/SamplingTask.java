package com.yx.lab.modules.sample.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("lab_sampling_task")
@EqualsAndHashCode(callSuper = true)
public class SamplingTask extends BaseEntity {

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

    private String onsiteMetrics;

    private String photoUrls;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedTime;

    private String remark;
}
