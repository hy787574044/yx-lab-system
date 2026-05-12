package com.yx.lab.modules.detection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检测方法实体，维护参数可选的标准方法、依据和适用范围。
 */
@Data
@TableName("lab_detection_method")
@EqualsAndHashCode(callSuper = true)
public class DetectionMethod extends BaseEntity {

    @Schema(description = "检测方法名称")
    @TableField("method_name")
    private String methodName;

    @Schema(description = "检测方法编码")
    @TableField("method_code")
    private String methodCode;

    @Schema(description = "绑定参数ID")
    @TableField("parameter_id")
    private Long parameterId;

    @Schema(description = "绑定参数名称")
    @TableField("parameter_name")
    private String parameterName;

    @Schema(description = "标准编号")
    @TableField("standard_code")
    private String standardCode;

    @Schema(description = "方法依据")
    @TableField("method_basis")
    private String methodBasis;

    @Schema(description = "适用范围")
    @TableField("apply_scope")
    private String applyScope;

    @Schema(description = "启用状态，1启用，0禁用")
    @TableField("enabled")
    private Integer enabled;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
