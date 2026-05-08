package com.yx.lab.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统角色实体。
 */
@Data
@TableName("lab_role")
@EqualsAndHashCode(callSuper = true)
public class LabRole extends BaseEntity {

    private String roleCode;

    private String roleName;

    private String roleScope;

    private Integer status;

    private String remark;
}
