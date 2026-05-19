package com.yx.lab.modules.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程配置下拉选项。
 */
@Data
@Schema(description = "流程配置下拉选项")
public class FlowConfigOptionVO {

    @Schema(description = "流程ID")
    private Long id;

    @Schema(description = "流程名称")
    private String flowName;

    @Schema(description = "流程类型")
    private String flowType;

    @Schema(description = "是否默认流程")
    private Boolean defaultFlag;
}
