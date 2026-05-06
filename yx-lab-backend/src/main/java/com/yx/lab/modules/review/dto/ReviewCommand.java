package com.yx.lab.modules.review.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ReviewCommand {

    @NotNull(message = "检测记录不能为空")
    private Long detectionRecordId;

    @NotBlank(message = "审核结果不能为空")
    private String reviewResult;

    private String rejectReason;

    private String reviewRemark;
}
