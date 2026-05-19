package com.yx.lab.modules.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程配置展示对象。
 */
@Data
@Schema(description = "流程配置展示对象")
public class FlowConfigVO {

    @Schema(description = "流程ID")
    private Long id;

    @Schema(description = "流程名称")
    private String flowName;

    @Schema(description = "流程类型")
    private String flowType;

    @Schema(description = "适用范围")
    private String scopeName;

    @Schema(description = "是否默认流程")
    private Boolean defaultFlag;

    @Schema(description = "状态，1 启用，0 停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "节点数量")
    private Integer nodeCount;

    @Schema(description = "节点列表")
    private List<FlowNodeVO> nodes = new ArrayList<>();

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
}
