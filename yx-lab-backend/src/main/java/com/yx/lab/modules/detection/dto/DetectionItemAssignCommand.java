package com.yx.lab.modules.detection.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 检测子项人员分配命令。
 */
@Data
public class DetectionItemAssignCommand {

    /**
     * 检测子项主键。
     */
    @NotNull(message = "检测子项ID不能为空")
    private Long itemId;

    /**
     * 绑定的检测员ID，传空表示清空当前分配。
     */
    private Long detectorId;
}
