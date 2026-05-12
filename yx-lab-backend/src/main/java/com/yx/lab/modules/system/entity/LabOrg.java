package com.yx.lab.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机构实体，维护系统组织架构及上下级关系。
 */
@Data
@TableName("lab_org")
@EqualsAndHashCode(callSuper = true)
public class LabOrg extends BaseEntity {

    @Schema(description = "机构编码")
    @TableField("org_code")
    private String orgCode;

    @Schema(description = "机构名称")
    @TableField("org_name")
    private String orgName;

    @Schema(description = "上级机构ID")
    @TableField("parent_id")
    private Long parentId;

    @Schema(description = "上级机构名称")
    @TableField("parent_name")
    private String parentName;

    @Schema(description = "机构类型")
    @TableField("org_type")
    private String orgType;

    @Schema(description = "状态，1启用，0停用")
    @TableField("status")
    private Integer status;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
