package com.yx.lab.modules.sample.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 状态数量统计展示对象。
 */
@Data
@Schema(description = "状态数量统计展示对象")
public class StatusCountVO {

    @Schema(description = "状态编码")
    private String status;

    @Schema(description = "数量")
    private Long count;
}
