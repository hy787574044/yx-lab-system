package com.yx.lab.modules.detection.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检测步骤实体，用于配置检测套餐对应的标准操作步骤。
 */
@Data
@TableName("lab_detection_step")
@EqualsAndHashCode(callSuper = true)
public class DetectionStep extends BaseEntity {

    @Schema(description = "检测套餐ID")
    @TableField("type_id")
    private Long typeId;

    @Schema(description = "检测套餐名称")
    @TableField("type_name")
    private String typeName;

    @Schema(description = "步骤名称")
    @TableField("step_name")
    private String stepName;

    @Schema(description = "步骤顺序")
    @TableField("step_order")
    private Integer stepOrder;

    @Schema(description = "步骤说明")
    @TableField("step_description")
    private String stepDescription;

    @Schema(description = "试剂要求")
    @TableField("reagent_requirement")
    private String reagentRequirement;

    @Schema(description = "操作要求")
    @TableField("operation_requirement")
    private String operationRequirement;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
