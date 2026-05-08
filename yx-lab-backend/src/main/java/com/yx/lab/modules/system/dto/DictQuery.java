package com.yx.lab.modules.system.dto;

import com.yx.lab.common.model.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据字典分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "数据字典分页查询条件")
public class DictQuery extends PageQuery {

    @Schema(description = "关键字，可按字典编码、字典名称、所属模块、字典项文本或备注模糊检索")
    private String keyword;

    @Schema(description = "状态，1 启用，0 停用")
    private Integer status;
}
