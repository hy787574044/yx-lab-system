package com.yx.lab.modules.system.dto;

import com.yx.lab.common.model.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程配置分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "流程配置分页查询条件")
public class FlowConfigQuery extends PageQuery {

    @Schema(description = "关键字，可按流程名称、适用范围、备注或节点信息检索")
    private String keyword;

    @Schema(description = "流程类型，REVIEW 审核流程，PUBLISH 发布流程")
    private String flowType;

    @Schema(description = "状态，1 启用，0 停用")
    private Integer status;
}
