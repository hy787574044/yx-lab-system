package com.yx.lab.modules.sample.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class SamplingTaskCompleteCommand {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    private String onsiteMetrics;

    private String weather;

    private String temperature;

    private String photoUrls;

    private String remark;

    private String address;

    private String latitude;

    private String longitude;

    private List<SampleDetectionConfigItem> detectionConfigItems;

    private String sampleTotalVolume;

    private String sampleBottleCount;
}
