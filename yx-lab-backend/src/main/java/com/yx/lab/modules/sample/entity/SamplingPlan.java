package com.yx.lab.modules.sample.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("lab_sampling_plan")
@EqualsAndHashCode(callSuper = true)
public class SamplingPlan extends BaseEntity {

    private String planName;

    private Long pointId;

    private String pointName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Long samplerId;

    private String samplerName;

    private String samplingType;

    private String sampleType;

    private String planStatus;

    private String remark;
}
