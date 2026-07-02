package com.yx.lab.modules.detection.entity;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("lab_detection_assignment_memory")
@EqualsAndHashCode(callSuper = true)
public class DetectionAssignmentMemory extends BaseEntity {

    @Schema(description = "所属机构ID")
    @TableField("org_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orgId;

    @Schema(description = "检测参数ID")
    @TableField("parameter_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parameterId;

    @Schema(description = "检测参数名称")
    @TableField("parameter_name")
    private String parameterName;

    @Schema(description = "检测人员ID")
    @TableField("detector_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long detectorId;

    @Schema(description = "检测人员名称")
    @TableField("detector_name")
    private String detectorName;
}
