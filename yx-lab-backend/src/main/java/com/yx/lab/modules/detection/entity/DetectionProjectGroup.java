package com.yx.lab.modules.detection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检测项目组实体，用于对检测套餐进行业务归类。
 */
@Data
@TableName("lab_detection_project_group")
@EqualsAndHashCode(callSuper = true)
public class DetectionProjectGroup extends BaseEntity {

    @Schema(description = "项目组名称")
    @TableField("group_name")
    private String groupName;

    @Schema(description = "启用状态，1启用，0禁用")
    @TableField("enabled")
    private Integer enabled;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
