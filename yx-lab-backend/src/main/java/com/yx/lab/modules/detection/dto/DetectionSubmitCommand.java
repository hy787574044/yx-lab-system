package com.yx.lab.modules.detection.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class DetectionSubmitCommand {

    @NotNull(message = "样品ID不能为空")
    private Long sampleId;

    @NotNull(message = "检测类型不能为空")
    private Long detectionTypeId;

    private String detectionTypeName;

    private String abnormalRemark;

    @Valid
    @NotEmpty(message = "检测项不能为空")
    private List<DetectionItemCommand> items;
}
