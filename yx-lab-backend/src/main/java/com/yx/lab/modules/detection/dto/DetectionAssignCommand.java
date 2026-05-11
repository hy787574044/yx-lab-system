package com.yx.lab.modules.detection.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 检测主流程下的子项分配命令。
 */
@Data
public class DetectionAssignCommand {

    /**
     * 本次需要保存的检测子项分配列表。
     */
    @Valid
    @NotEmpty(message = "检测子项分配不能为空")
    private List<DetectionItemAssignCommand> items;
}
