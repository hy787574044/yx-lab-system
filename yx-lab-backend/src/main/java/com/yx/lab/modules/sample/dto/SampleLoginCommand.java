package com.yx.lab.modules.sample.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class SampleLoginCommand {

    private Long taskId;

    /**
     * 现场封签 OCR 识别号，或人工输入的封签号。
     * 有值时优先按封签号回溯采样任务。
     */
    private String sealNo;

    @NotNull(message = "采样点位不能为空")
    private Long pointId;

    @NotBlank(message = "采样点位名称不能为空")
    private String pointName;

    @NotBlank(message = "样品类型不能为空")
    private String sampleType;

    @NotBlank(message = "检测项目不能为空")
    private String detectionItems;

    @NotNull(message = "采样时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime samplingTime;

    @NotNull(message = "采样人不能为空")
    private Long samplerId;

    @NotBlank(message = "采样人姓名不能为空")
    private String samplerName;

    private String weather;

    private String storageCondition;

    private String remark;
}
