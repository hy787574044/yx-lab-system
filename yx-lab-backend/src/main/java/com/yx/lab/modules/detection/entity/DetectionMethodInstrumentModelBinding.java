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
@TableName("lab_detection_method_instrument_model_binding")
@EqualsAndHashCode(callSuper = true)
public class DetectionMethodInstrumentModelBinding extends BaseEntity {

    @Schema(description = "检测方法ID")
    @TableField("method_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long methodId;

    @Schema(description = "检测方法名称")
    @TableField("method_name")
    private String methodName;

    @Schema(description = "设备型号")
    @TableField("instrument_model")
    private String instrumentModel;

    @Schema(description = "设备名称/设备型号展示")
    @TableField(exist = false)
    private String instrumentDisplayNames;

    @Schema(description = "生产厂家")
    @TableField("manufacturer")
    private String manufacturer;

    @Schema(description = "当前型号在库设备数量")
    @TableField("instrument_count")
    private Integer instrumentCount;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
