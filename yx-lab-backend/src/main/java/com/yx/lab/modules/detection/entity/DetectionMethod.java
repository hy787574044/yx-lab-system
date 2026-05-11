package com.yx.lab.modules.detection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检测方法台账实体。
 */
@Data
@TableName("lab_detection_method")
@EqualsAndHashCode(callSuper = true)
public class DetectionMethod extends BaseEntity {

    private String methodName;

    private String methodCode;

    private Long parameterId;

    private String parameterName;

    private String standardCode;

    private String methodBasis;

    private String applyScope;

    private Integer enabled;

    private String remark;
}
