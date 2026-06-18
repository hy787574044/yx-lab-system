package com.yx.lab.modules.system.controller;

import com.yx.lab.common.config.LabSecurityProperties;
import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.modules.system.dto.EmbedLoginRequest;
import com.yx.lab.modules.system.dto.LoginRequest;
import com.yx.lab.modules.system.dto.PasswordChangeCommand;
import com.yx.lab.modules.system.dto.UserProfileUpdateCommand;
import com.yx.lab.modules.system.service.AuthService;
import com.yx.lab.modules.system.vo.CaptchaVO;
import com.yx.lab.modules.system.vo.LoginVO;
import com.yx.lab.modules.system.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final LabSecurityProperties securityProperties;

    @GetMapping("/captcha")
    @Operation(summary = "获取登录验证码")
    public ApiResponse<CaptchaVO> captcha() {
        return ApiResponse.success(authService.createCaptcha());
    }

    @PostMapping("/login")
    @Operation(summary = "PC 端登录")
    public ApiResponse<LoginVO> login(@Validated @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request, "PC"));
    }

    @PostMapping("/embedLogin")
    @Operation(summary = "第三方嵌入登录")
    public ApiResponse<LoginVO> embedLogin(@Validated @RequestBody EmbedLoginRequest request) {
        return ApiResponse.success("登录成功", authService.embedLogin(request));
    }

    @PostMapping("/accessCheckLogin")
    @Operation(summary = "第三方跳转令牌校验登录")
    public ApiResponse<LoginVO> accessCheckLogin(@Validated @RequestBody EmbedLoginRequest request) {
        return ApiResponse.success("登录成功", authService.accessCheckLogin(request));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前登录人信息")
    public ApiResponse<UserProfileVO> me() {
        return ApiResponse.success(authService.me());
    }

    @PostMapping("/profile")
    @Operation(summary = "修改当前登录人资料")
    public ApiResponse<UserProfileVO> updateProfile(@Valid @RequestBody UserProfileUpdateCommand command,
                                                    HttpServletRequest request) {
        return ApiResponse.success("资料修改成功", authService.updateProfile(command, resolveToken(request)));
    }

    @PostMapping("/changePassword")
    @Operation(summary = "修改当前登录人密码")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeCommand command) {
        authService.changePassword(command);
        return ApiResponse.successMessage("密码修改成功");
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(resolveToken(request));
        return ApiResponse.successMessage("退出成功");
    }

    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader(securityProperties.getTokenHeader());
        if (authHeader == null || !authHeader.startsWith(securityProperties.getTokenPrefix())) {
            return "";
        }
        return authHeader.substring(securityProperties.getTokenPrefix().length()).trim();
    }
}
