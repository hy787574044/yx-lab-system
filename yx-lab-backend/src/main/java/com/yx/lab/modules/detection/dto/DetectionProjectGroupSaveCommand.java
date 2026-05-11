package com.yx.lab.modules.detection.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 检测项目组保存命令。
 */
@Data
public class DetectionProjectGroupSaveCommand {

    @NotBlank(message = "检测项目组名称不能为空")
    private String groupName;

    @NotNull(message = "启用状态不能为空")
    private Integer enabled;

    private String remark;

    private List<Long> projectIds;
}
