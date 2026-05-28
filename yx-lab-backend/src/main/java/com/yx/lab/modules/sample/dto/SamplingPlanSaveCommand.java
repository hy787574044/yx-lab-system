package com.yx.lab.modules.sample.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SamplingPlanSaveCommand {

    @NotBlank(message = "计划名称不能为空")
    private String planName;

    private Long pointId;

    @NotBlank(message = "采样点位名称不能为空")
    private String pointName;

    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Long samplerId;

    private String samplerName;

    private String samplingType;

    @NotBlank(message = "样品类型不能为空")
    private String sampleType;

    @NotNull(message = "检测套餐不能为空")
    private Long detectionTypeId;

    private String detectionTypeName;

    private List<SampleDetectionConfigItem> detectionConfigItems;

    private List<String> samplingBasisList;

    private String cycleType;

    private String planStatus;

    private String remark;
}
