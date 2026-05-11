package com.yx.lab.modules.review.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ReviewItemCommand {

    @NotNull(message = "检测子流程不能为空")
    private Long itemId;

    @NotBlank(message = "子流程审核结果不能为空")
    private String reviewResult;

    private String rejectReason;

    private String reviewRemark;
}
