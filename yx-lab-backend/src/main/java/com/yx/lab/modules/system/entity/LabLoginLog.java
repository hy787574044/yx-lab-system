package com.yx.lab.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 登录日志实体，记录用户登录结果、来源和时间信息。
 */
@Data
@TableName("lab_login_log")
@EqualsAndHashCode(callSuper = true)
public class LabLoginLog extends BaseEntity {

    /**
     * 登录用户ID。
     */
    @Schema(description = "用户ID")
    @TableField("user_id")
    private Long userId;

    /**
     * 登录账号。
     */
    @Schema(description = "用户名")
    @TableField("username")
    private String username;

    /**
     * 登录人姓名。
     */
    @Schema(description = "姓名")
    @TableField("real_name")
    private String realName;

    /**
     * 登录人角色编码。
     */
    @Schema(description = "角色编码")
    @TableField("role_code")
    private String roleCode;

    /**
     * 登录渠道，PC / MOBILE。
     */
    @Schema(description = "登录渠道")
    @TableField("login_channel")
    private String loginChannel;

    /**
     * 登录状态。
     */
    @Schema(description = "登录状态")
    @TableField("login_status")
    private String loginStatus;

    /**
     * 登录时间。
     */
    @Schema(description = "登录时间")
    @TableField("login_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;

    /**
     * 备注说明。
     */
    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
