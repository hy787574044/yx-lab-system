package com.yx.lab.modules.sample.entity;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("lab_sampling_plan")
@EqualsAndHashCode(callSuper = true)
public class SamplingPlan extends BaseEntity {

    @Schema(description = "计划名称")
    @TableField("plan_name")
    private String planName;

    @Schema(description = "所属机构ID")
    @TableField("org_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orgId;

    @Schema(description = "监测点位ID")
    @TableField("point_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long pointId;

    @Schema(description = "监测点位名称")
    @TableField("point_name")
    private String pointName;

    @Schema(description = "点位地址/地图名称")
    @TableField("address")
    private String address;

    @Schema(description = "纬度")
    @TableField("latitude")
    private String latitude;

    @Schema(description = "经度")
    @TableField("longitude")
    private String longitude;

    @Schema(description = "开始时间")
    @TableField("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @TableField("end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "采样员ID")
    @TableField("sampler_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long samplerId;

    @Schema(description = "采样员ID集合")
    @TableField("sampler_ids")
    private String samplerIds;

    @Schema(description = "采样员名称")
    @TableField("sampler_name")
    private String samplerName;

    @Schema(description = "采样方式")
    @TableField("sampling_type")
    private String samplingType;

    @Schema(description = "样品类型")
    @TableField("sample_type")
    private String sampleType;

    @Schema(description = "检测套餐ID")
    @TableField("detection_type_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long detectionTypeId;

    @Schema(description = "检测套餐名称")
    @TableField("detection_type_name")
    private String detectionTypeName;

    @Schema(description = "检测配置快照")
    @TableField("detection_config_snapshot")
    private String detectionConfigSnapshot;

    @Schema(description = "周期类型")
    @TableField("cycle_type")
    private String cycleType;

    @Schema(description = "计划状态")
    @TableField("plan_status")
    private String planStatus;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
