package com.yx.lab.modules.detection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("lab_detection_type")
@EqualsAndHashCode(callSuper = true)
public class DetectionType extends BaseEntity {

    private String typeName;

    private String parameterIds;

    private String parameterNames;

    private Integer enabled;

    private String remark;
}
