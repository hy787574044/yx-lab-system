package com.yx.lab.modules.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统日志展示对象。
 */
@Data
@Schema(description = "系统日志展示对象")
public class SystemLogVO {

    @Schema(description = "日志唯一标识")
    private String id;

    @Schema(description = "日志来源类型")
    private String sourceType;

    @Schema(description = "日志来源名称")
    private String sourceName;

    @Schema(description = "业务编号")
    private String businessNo;

    @Schema(description = "操作人")
    private String operatorName;

    @Schema(description = "日志内容")
    private String content;

    @Schema(description = "日志状态")
    private String status;

    @Schema(description = "发生时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime occurTime;
}
