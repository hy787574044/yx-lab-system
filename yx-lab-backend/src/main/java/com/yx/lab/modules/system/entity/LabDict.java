package com.yx.lab.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据字典实体。
 */
@Data
@TableName("lab_dict")
@EqualsAndHashCode(callSuper = true)
public class LabDict extends BaseEntity {

    private String dictCode;

    private String dictName;

    private String moduleName;

    private String itemText;

    private Integer status;

    private String remark;
}
