package com.yx.lab.modules.sample.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SamplingPlanDispatchCommand {

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime samplingTime;

    private List<Long> samplerIds;

    private Long samplerId;

    private String samplerName;
}
