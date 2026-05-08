package com.yx.lab.modules.system.dto;

import com.yx.lab.common.model.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户管理分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户管理分页查询条件")
public class UserQuery extends PageQuery {

    @Schema(description = "关键字，可按用户名、姓名、手机号、角色编码模糊检索")
    private String keyword;

    @Schema(description = "状态，1 启用，0 停用")
    private Integer status;
}
