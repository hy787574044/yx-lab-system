package com.yx.lab.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 登录日志实体。
 */
@Data
@TableName("lab_login_log")
@EqualsAndHashCode(callSuper = true)
public class LabLoginLog extends BaseEntity {

    /**
     * 登录用户ID。
     */
    private Long userId;

    /**
     * 登录账号。
     */
    private String username;

    /**
     * 登录人姓名。
     */
    private String realName;

    /**
     * 登录人角色编码。
     */
    private String roleCode;

    /**
     * 登录渠道，PC / MOBILE。
     */
    private String loginChannel;

    /**
     * 登录状态。
     */
    private String loginStatus;

    /**
     * 登录时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;

    /**
     * 备注说明。
     */
    private String remark;
}
