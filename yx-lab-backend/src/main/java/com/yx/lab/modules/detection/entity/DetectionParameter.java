package com.yx.lab.modules.detection.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 检测参数实体，维护 pH、浊度、余氯等参数基础定义。
 */
@Data
@TableName("lab_detection_parameter")
@EqualsAndHashCode(callSuper = true)
public class DetectionParameter extends BaseEntity {

    @Schema(description = "检测参数名称")
    @TableField("parameter_name")
    private String parameterName;

    @Schema(description = "标准下限")
    @TableField("standard_min")
    private BigDecimal standardMin;

    @Schema(description = "标准上限")
    @TableField("standard_max")
    private BigDecimal standardMax;

    @Schema(description = "单位")
    @TableField("unit")
    private String unit;

    @Schema(description = "超限规则")
    @TableField("exceed_rule")
    private String exceedRule;

    @Schema(description = "参考范围")
    @TableField("reference_standard")
    private String referenceStandard;

    @Schema(description = "启用状态，1启用，0禁用")
    @TableField("enabled")
    private Integer enabled;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
