package com.yx.lab.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("lab_user")
@EqualsAndHashCode(callSuper = true)
public class LabUser extends BaseEntity {

    private String username;

    private String password;

    private String realName;

    private String roleCode;

    private String phone;

    private Integer status;
}
