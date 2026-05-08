package com.yx.lab.modules.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机构管理展示对象。
 */
@Data
@Schema(description = "机构管理展示对象")
public class LabOrgVO {

    @Schema(description = "机构ID")
    private Long id;

    @Schema(description = "机构编码")
    private String orgCode;

    @Schema(description = "机构名称")
    private String orgName;

    @Schema(description = "上级机构ID")
    private Long parentId;

    @Schema(description = "上级机构名称")
    private String parentName;

    @Schema(description = "机构类型")
    private String orgType;

    @Schema(description = "状态，1 启用，0 停用")
    private Integer status;

    @Schema(description = "成员数")
    private Long memberCount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
}
