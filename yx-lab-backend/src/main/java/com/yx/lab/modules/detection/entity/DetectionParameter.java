package com.yx.lab.modules.detection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@TableName("lab_detection_parameter")
@EqualsAndHashCode(callSuper = true)
public class DetectionParameter extends BaseEntity {

    private String parameterName;

    private BigDecimal standardMin;

    private BigDecimal standardMax;

    private String unit;

    private String exceedRule;

    private String referenceStandard;

    private Integer enabled;

    private String remark;
}
