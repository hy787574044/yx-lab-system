package com.yx.lab.modules.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据字典项展示对象，供业务页面读取可选项。
 */
@Data
@Schema(description = "数据字典项展示对象")
public class DictItemVO {

    @Schema(description = "字典项值")
    private String value;

    @Schema(description = "字典项显示文本")
    private String label;
}
