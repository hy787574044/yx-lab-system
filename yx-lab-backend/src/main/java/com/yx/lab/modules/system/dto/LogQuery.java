package com.yx.lab.modules.system.dto;

import com.yx.lab.common.model.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统日志分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统日志分页查询条件")
public class LogQuery extends PageQuery {

    @Schema(description = "关键字，可按来源名称、业务编号、操作人、日志内容或状态模糊检索")
    private String keyword;

    @Schema(description = "日志来源类型，可选值：LOGIN、PROCESS、SAMPLE、DETECTION、REVIEW、PUSH")
    private String sourceType;
}
