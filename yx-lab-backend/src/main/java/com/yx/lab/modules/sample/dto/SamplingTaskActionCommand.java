package com.yx.lab.modules.sample.dto;

import lombok.Data;

@Data
public class SamplingTaskActionCommand {

    /**
     * 采样任务封签号。
     * 可在开始任务时一并传入，也可通过单独的封签录入接口维护。
     */
    private String sealNo;

    private String reason;

    private String remark;
}
