package com.yx.lab.modules.sample.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class MonitoringPointSaveCommand {

    @NotBlank(message = "点位名称不能为空")
    private String pointName;

    private String address;

    private String longitude;

    private String latitude;

    private String regionName;

    @NotBlank(message = "点位类型不能为空")
    private String pointType;

    @NotBlank(message = "点位状态不能为空")
    private String pointStatus;
}
