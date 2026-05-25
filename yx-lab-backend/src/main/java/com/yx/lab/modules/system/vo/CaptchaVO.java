package com.yx.lab.modules.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录验证码")
public class CaptchaVO {

    @Schema(description = "验证码唯一标识")
    private String captchaId;

    @Schema(description = "验证码图片 Base64 数据")
    private String imageBase64;
}
