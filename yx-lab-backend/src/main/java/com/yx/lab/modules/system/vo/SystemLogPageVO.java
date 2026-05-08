package com.yx.lab.modules.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 系统日志分页结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "系统日志分页结果")
public class SystemLogPageVO {

    @Schema(description = "分页总数")
    private long total;

    @Schema(description = "日志记录")
    private List<SystemLogVO> records;

    @Schema(description = "关键字条件下的全部日志数")
    private long totalCount;

    @Schema(description = "关键字条件下的登录日志数")
    private long loginCount;

    @Schema(description = "关键字条件下的流程日志数")
    private long processCount;

    @Schema(description = "关键字条件下的推送日志数")
    private long pushCount;
}
