package com.yx.lab.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 当前登录人资料修改命令。
 */
@Data
@Schema(description = "当前登录人资料修改命令")
public class UserProfileUpdateCommand {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过64个字符")
    @Schema(description = "姓名", required = true)
    private String realName;

    @Size(max = 32, message = "手机号长度不能超过32个字符")
    @Schema(description = "手机号")
    private String phone;

    @Size(max = 500, message = "头像地址长度不能超过500个字符")
    @Schema(description = "头像地址")
    private String avatarUrl;
}
