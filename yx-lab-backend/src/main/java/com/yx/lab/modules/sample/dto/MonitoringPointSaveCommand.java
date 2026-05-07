package com.yx.lab.modules.sample.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class MonitoringPointSaveCommand {

    @NotBlank(message = "点位名称不能为空")
    private String pointName;

    private String longitude;

    private String latitude;

    private String regionName;

    private Integer servicePopulation;

    @NotBlank(message = "监测频次不能为空")
    private String frequencyType;

    private Long ownerId;

    private String ownerName;

    private String contactPhone;

    @NotBlank(message = "点位类型不能为空")
    private String pointType;

    @NotBlank(message = "点位状态不能为空")
    private String pointStatus;
}
