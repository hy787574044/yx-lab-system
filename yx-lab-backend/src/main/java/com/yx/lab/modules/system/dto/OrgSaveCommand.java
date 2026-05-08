package com.yx.lab.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 机构新增或编辑命令。
 */
@Data
@Schema(description = "机构新增或编辑命令")
public class OrgSaveCommand {

    @NotBlank(message = "机构编码不能为空")
    @Size(max = 32, message = "机构编码长度不能超过32个字符")
    @Schema(description = "机构编码", required = true)
    private String orgCode;

    @NotBlank(message = "机构名称不能为空")
    @Size(max = 64, message = "机构名称长度不能超过64个字符")
    @Schema(description = "机构名称", required = true)
    private String orgName;

    @Schema(description = "上级机构ID")
    private Long parentId;

    @Size(max = 64, message = "机构类型长度不能超过64个字符")
    @Schema(description = "机构类型")
    private String orgType;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态，1 启用，0 停用", required = true)
    private Integer status;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Schema(description = "备注")
    private String remark;
}
