package com.yx.lab.modules.detection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("lab_detection_step")
@EqualsAndHashCode(callSuper = true)
public class DetectionStep extends BaseEntity {

    private Long typeId;

    private String typeName;

    private String stepName;

    private Integer stepOrder;

    private String stepDescription;

    private String reagentRequirement;

    private String operationRequirement;

    private String remark;
}
