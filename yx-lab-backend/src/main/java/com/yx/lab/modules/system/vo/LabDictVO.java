package com.yx.lab.modules.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据字典展示对象。
 */
@Data
@Schema(description = "数据字典展示对象")
public class LabDictVO {

    @Schema(description = "字典ID")
    private Long id;

    @Schema(description = "字典编码")
    private String dictCode;

    @Schema(description = "字典名称")
    private String dictName;

    @Schema(description = "所属模块")
    private String moduleName;

    @Schema(description = "字典项数量")
    private Integer itemCount;

    @Schema(description = "字典项文本")
    private String itemText;

    @Schema(description = "状态，1 启用，0 停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
}
