package com.yx.lab.modules.system.entity;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程节点实体，维护流程模板下的审批节点顺序和处理规则。
 */
@Data
@TableName("lab_flow_node")
@EqualsAndHashCode(callSuper = true)
public class LabFlowNode extends BaseEntity {

    @Schema(description = "流程配置ID")
    @TableField("flow_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long flowId;

    @Schema(description = "节点顺序")
    @TableField("node_order")
    private Integer nodeOrder;

    @Schema(description = "节点名称")
    @TableField("node_name")
    private String nodeName;

    @Schema(description = "审批角色")
    @TableField("role_name")
    private String roleName;

    @Schema(description = "审批角色编码")
    @TableField("role_code")
    private String roleCode;

    @Schema(description = "指定人员ID")
    @TableField("assignee_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long assigneeId;

    @Schema(description = "指定人员")
    @TableField("assignee_name")
    private String assigneeName;

    @Schema(description = "是否必审，1是，0否")
    @TableField("required_flag")
    private Integer requiredFlag;

    @Schema(description = "驳回方式，PREVIOUS退回上一步，DETECTION退回检测，TERMINATE流程终止")
    @TableField("reject_mode")
    private String rejectMode;
}
