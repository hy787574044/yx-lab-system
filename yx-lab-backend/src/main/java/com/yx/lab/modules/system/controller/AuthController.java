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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "PC 登录")
    public ApiResponse<LoginVO> login(@Validated @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    @PostMapping("/mobile-login")
    @Operation(summary = "移动端登录")
    public ApiResponse<LoginVO> mobileLogin(@Validated @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "当前登录人")
    public ApiResponse<UserProfileVO> me() {
        return ApiResponse.success(authService.me());
    }
}
