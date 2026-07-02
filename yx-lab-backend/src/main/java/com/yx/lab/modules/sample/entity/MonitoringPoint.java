package com.yx.lab.modules.sample.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("lab_monitoring_point")
@EqualsAndHashCode(callSuper = true)
public class MonitoringPoint extends BaseEntity {

    @Schema(description = "点位名称")
    @TableField("point_name")
    private String pointName;

    @Schema(description = "点位地址/地图名称")
    @TableField("address")
    private String address;

    @Schema(description = "经度")
    @TableField("longitude")
    private String longitude;

    @Schema(description = "纬度")
    @TableField("latitude")
    private String latitude;

    @Schema(description = "所属机构ID")
    @TableField("org_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orgId;

    @Schema(description = "所属机构名称（关联查询）")
    @TableField(exist = false)
    private String orgName;

    @Schema(description = "点位类型")
    @TableField("point_type")
    private String pointType;

    @Schema(description = "点位状态")
    @TableField("point_status")
    private String pointStatus;
}
