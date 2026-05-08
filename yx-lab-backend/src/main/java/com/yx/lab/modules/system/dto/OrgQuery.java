package com.yx.lab.modules.system.dto;

import com.yx.lab.common.model.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机构管理分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "机构管理分页查询条件")
public class OrgQuery extends PageQuery {

    @Schema(description = "关键字，可按机构编码、机构名称、上级机构、机构类型或备注模糊检索")
    private String keyword;

    @Schema(description = "状态，1 启用，0 停用")
    private Integer status;
}
