package com.yx.lab.modules.review.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ReviewCommand {

    @NotNull(message = "检测记录不能为空")
    private Long detectionRecordId;

    @NotBlank(message = "审核结果不能为空")
    private String reviewResult;

    private String rejectReason;

    private String reviewRemark;

    @Valid
    @NotEmpty(message = "子流程审核明细不能为空")
    private List<ReviewItemCommand> items;
}
