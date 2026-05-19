package com.yx.lab.modules.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程节点展示对象。
 */
@Data
@Schema(description = "流程节点展示对象")
public class FlowNodeVO {

    @Schema(description = "节点ID")
    private Long id;

    @Schema(description = "节点顺序")
    private Integer nodeOrder;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "审批角色")
    private String roleName;

    @Schema(description = "审批角色编码")
    private String roleCode;

    @Schema(description = "指定人员ID")
    private Long assigneeId;

    @Schema(description = "指定人员")
    private String assigneeName;

    @Schema(description = "是否必审")
    private Boolean required;

    @Schema(description = "驳回方式")
    private String rejectMode;
}
