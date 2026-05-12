package com.yx.lab.modules.detection.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检测套餐实体，定义样品登录时可选的一组检测参数及其方法绑定。
 */
@Data
@TableName("lab_detection_type")
@EqualsAndHashCode(callSuper = true)
public class DetectionType extends BaseEntity {

    @Schema(description = "检测套餐名称")
    @TableField("type_name")
    private String typeName;

    @Schema(description = "项目组ID")
    @TableField("group_id")
    private Long groupId;

    @Schema(description = "项目组名称")
    @TableField("group_name")
    private String groupName;

    @Schema(description = "默认检测员ID")
    @TableField("detector_id")
    private Long detectorId;

    @Schema(description = "默认检测员名称")
    @TableField("detector_name")
    private String detectorName;

    @Schema(description = "参数ID集合文本")
    @TableField("parameter_ids")
    private String parameterIds;

    @Schema(description = "参数名称集合文本")
    @TableField("parameter_names")
    private String parameterNames;

    /**
     * 检测套餐内“检测参数 -> 检测方法”绑定关系，JSON 数组文本。
     */
    @Schema(description = "参数方法绑定关系JSON")
    @TableField("parameter_method_bindings")
    private String parameterMethodBindings;

    /**
     * 前端列表展示用的参数-方法摘要，不落库。
     */
    @Schema(description = "参数方法摘要，不落库")
    @TableField(exist = false)
    private String parameterMethodNames;

    @Schema(description = "启用状态，1启用，0禁用")
    @TableField("enabled")
    private Integer enabled;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
