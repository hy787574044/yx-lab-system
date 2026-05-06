package com.yx.lab.modules.sample.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class SamplingPlanDispatchCommand {

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    private LocalDateTime samplingTime;
}
