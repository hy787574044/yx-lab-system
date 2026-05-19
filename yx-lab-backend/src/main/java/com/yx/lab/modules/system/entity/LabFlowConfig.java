package com.yx.lab.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程配置实体，维护审核流程、发布流程等可选流程模板。
 */
@Data
@TableName("lab_flow_config")
@EqualsAndHashCode(callSuper = true)
public class LabFlowConfig extends BaseEntity {

    @Schema(description = "流程名称")
    @TableField("flow_name")
    private String flowName;

    @Schema(description = "流程类型，REVIEW 审核流程，PUBLISH 发布流程")
    @TableField("flow_type")
    private String flowType;

    @Schema(description = "适用范围")
    @TableField("scope_name")
    private String scopeName;

    @Schema(description = "是否默认流程，1是，0否")
    @TableField("default_flag")
    private Integer defaultFlag;

    @Schema(description = "状态，1启用，0停用")
    @TableField("status")
    private Integer status;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
