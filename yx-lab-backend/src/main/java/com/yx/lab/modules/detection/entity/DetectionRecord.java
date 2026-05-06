package com.yx.lab.modules.detection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("lab_detection_record")
@EqualsAndHashCode(callSuper = true)
public class DetectionRecord extends BaseEntity {

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
