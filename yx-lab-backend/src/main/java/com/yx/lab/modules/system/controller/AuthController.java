package com.yx.lab.modules.system.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.modules.system.dto.LoginRequest;
import com.yx.lab.modules.system.service.AuthService;
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

/**
 * 认证控制器。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * PC 端登录。
     *
     * @param request 登录请求体
     * @return 登录结果
     */
    @PostMapping("/login")
    @Operation(summary = "PC 端登录")
    public ApiResponse<LoginVO> login(@Validated @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request, "PC"));
    }

    /**
     * 移动端登录。
     *
     * @param request 登录请求体
     * @return 登录结果
     */
    @PostMapping("/mobileLogin")
    @Operation(summary = "移动端登录")
    public ApiResponse<LoginVO> mobileLogin(@Validated @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request, "MOBILE"));
    }

    /**
     * 获取当前登录人信息。
     *
     * @return 当前登录人信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前登录人信息")
    public ApiResponse<UserProfileVO> me() {
        return ApiResponse.success(authService.me());
    }
}
