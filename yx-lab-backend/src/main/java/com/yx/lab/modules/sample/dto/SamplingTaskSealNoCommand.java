package com.yx.lab.modules.sample.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class SamplingTaskSealNoCommand {

    /**
     * 采样任务封签号。
     * 支持人工录入，也支持 OCR 识别结果回填。
     */
    @NotBlank(message = "封签号不能为空")
    private String sealNo;
}
