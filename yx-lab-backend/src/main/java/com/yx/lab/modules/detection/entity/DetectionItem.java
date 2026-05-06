package com.yx.lab.modules.detection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@TableName("lab_detection_item")
@EqualsAndHashCode(callSuper = true)
public class DetectionItem extends BaseEntity {

    private Long recordId;

    private Long parameterId;

    private String parameterName;

    private BigDecimal standardMin;

    private BigDecimal standardMax;

    private BigDecimal resultValue;

    private String unit;

    private Integer exceedFlag;
}
