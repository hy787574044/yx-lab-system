package com.yx.lab.modules.sample.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("lab_sample")
@EqualsAndHashCode(callSuper = true)
public class LabSample extends BaseEntity {

    private String sampleNo;

    private Long taskId;

    private Long pointId;

    private String pointName;

    private String sampleType;

    private String detectionItems;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime samplingTime;

    private Long samplerId;

    private String samplerName;

    private String weather;

    private String storageCondition;

    private String sampleStatus;

    private String resultSummary;

    private String remark;
}
