package com.yx.lab.modules.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 机构下拉选项。
 */
@Data
@Schema(description = "机构下拉选项")
public class OrgOptionVO {

    @Schema(description = "机构ID")
    private Long id;

    @Schema(description = "机构编码")
    private String orgCode;

    @Schema(description = "机构名称")
    private String orgName;
}
