package com.yx.lab.modules.sample.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("lab_monitoring_point")
@EqualsAndHashCode(callSuper = true)
public class MonitoringPoint extends BaseEntity {

    private String pointName;

    private String longitude;

    private String latitude;

    private String regionName;

    private Integer servicePopulation;

    private String frequencyType;

    private Long ownerId;

    private String ownerName;

    private String contactPhone;

    private String pointType;

    private String pointStatus;
}
