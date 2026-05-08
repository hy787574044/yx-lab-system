package com.yx.lab.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 数据字典新增或编辑命令。
 */
@Data
@Schema(description = "数据字典新增或编辑命令")
public class DictSaveCommand {

    @NotBlank(message = "字典编码不能为空")
    @Size(max = 64, message = "字典编码长度不能超过64个字符")
    @Schema(description = "字典编码", required = true)
    private String dictCode;

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 64, message = "字典名称长度不能超过64个字符")
    @Schema(description = "字典名称", required = true)
    private String dictName;

    @NotBlank(message = "所属模块不能为空")
    @Size(max = 64, message = "所属模块长度不能超过64个字符")
    @Schema(description = "所属模块", required = true)
    private String moduleName;

    @Size(max = 4000, message = "字典项内容长度不能超过4000个字符")
    @Schema(description = "字典项文本，建议按换行分隔每一项")
    private String itemText;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态，1 启用，0 停用", required = true)
    private Integer status;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Schema(description = "备注")
    private String remark;
}
