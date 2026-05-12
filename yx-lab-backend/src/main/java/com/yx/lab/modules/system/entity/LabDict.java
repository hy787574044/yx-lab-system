package com.yx.lab.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据字典实体，维护系统中状态、类型等可配置字典项。
 */
@Data
@TableName("lab_dict")
@EqualsAndHashCode(callSuper = true)
public class LabDict extends BaseEntity {

    @Schema(description = "字典编码")
    @TableField("dict_code")
    private String dictCode;

    @Schema(description = "字典名称")
    @TableField("dict_name")
    private String dictName;

    @Schema(description = "所属模块")
    @TableField("module_name")
    private String moduleName;

    @Schema(description = "字典项文本")
    @TableField("item_text")
    private String itemText;

    @Schema(description = "状态，1启用，0停用")
    @TableField("status")
    private Integer status;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
