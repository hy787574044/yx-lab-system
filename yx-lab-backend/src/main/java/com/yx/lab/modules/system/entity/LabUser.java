package com.yx.lab.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体，维护系统账号、所属机构及角色信息。
 */
@Data
@TableName("lab_user")
@EqualsAndHashCode(callSuper = true)
public class LabUser extends BaseEntity {

    @Schema(description = "用户名")
    @TableField("username")
    private String username;

    @Schema(description = "登录密码")
    @TableField("password")
    private String password;

    @Schema(description = "姓名")
    @TableField("real_name")
    private String realName;

    @Schema(description = "所属机构ID")
    @TableField("org_id")
    private Long orgId;

    @Schema(description = "所属机构名称")
    @TableField("org_name")
    private String orgName;

    @Schema(description = "角色编码")
    @TableField("role_code")
    private String roleCode;

    @Schema(description = "手机号")
    @TableField("phone")
    private String phone;

    @Schema(description = "状态，1启用，0停用")
    @TableField("status")
    private Integer status;
}
