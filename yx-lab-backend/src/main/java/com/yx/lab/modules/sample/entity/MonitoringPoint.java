package com.yx.lab.modules.sample.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 监测点位实体，记录客户点位、频次及基础属性信息。
 */
@Data
@TableName("lab_monitoring_point")
@EqualsAndHashCode(callSuper = true)
public class MonitoringPoint extends BaseEntity {

    @Schema(description = "点位名称")
    @TableField("point_name")
    private String pointName;

    @Schema(description = "经度")
    @TableField("longitude")
    private String longitude;

    @Schema(description = "纬度")
    @TableField("latitude")
    private String latitude;

    @Schema(description = "区域名称")
    @TableField("region_name")
    private String regionName;

    @Schema(description = "服务人口")
    @TableField("service_population")
    private Integer servicePopulation;

    @Schema(description = "监测频次")
    @TableField("frequency_type")
    private String frequencyType;

    @Schema(description = "负责人ID")
    @TableField("owner_id")
    private Long ownerId;

    @Schema(description = "负责人名称")
    @TableField("owner_name")
    private String ownerName;

    @Schema(description = "联系电话")
    @TableField("contact_phone")
    private String contactPhone;

    @Schema(description = "点位类型")
    @TableField("point_type")
    private String pointType;

    @Schema(description = "点位状态")
    @TableField("point_status")
    private String pointStatus;
}
