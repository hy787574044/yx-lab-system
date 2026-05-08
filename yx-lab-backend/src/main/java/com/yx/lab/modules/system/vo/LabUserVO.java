package com.yx.lab.modules.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户管理展示对象。
 */
@Data
@Schema(description = "用户管理展示对象")
public class LabUserVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "姓名")
    private String realName;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "状态，1 启用，0 停用")
    private Integer status;

    @Schema(description = "创建人")
    private String createdName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @Schema(description = "更新人")
    private String updatedName;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
}
