package com.yx.lab.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机构实体。
 */
@Data
@TableName("lab_org")
@EqualsAndHashCode(callSuper = true)
public class LabOrg extends BaseEntity {

    private String orgCode;

    private String orgName;

    private Long parentId;

    private String parentName;

    private String orgType;

    private Integer status;

    private String remark;
}
