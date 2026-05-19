package com.yx.lab.modules.mobile.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.config.LabSecurityProperties;
import com.yx.lab.modules.system.dto.PasswordChangeCommand;
import com.yx.lab.modules.system.dto.UserProfileUpdateCommand;
import com.yx.lab.modules.system.service.AuthService;
import com.yx.lab.modules.system.vo.UserProfileVO;
import com.yx.lab.modules.mobile.service.MobileDetectionQueryService;
import com.yx.lab.modules.mobile.service.MobileReportQueryService;
import com.yx.lab.modules.mobile.service.MobileReviewQueryService;
import com.yx.lab.modules.mobile.service.MobileSamplingQueryService;
import com.yx.lab.modules.mobile.vo.MobileDetectionHistoryVO;
import com.yx.lab.modules.mobile.vo.MobileDetectionTodoVO;
import com.yx.lab.modules.mobile.vo.MobileReportVO;
import com.yx.lab.modules.mobile.vo.MobileReviewHistoryVO;
import com.yx.lab.modules.mobile.vo.MobileReviewTodoVO;
import com.yx.lab.modules.mobile.vo.MobileSamplingTodoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 移动端工作台控制器。
 * 聚合采样、检测、审核和报告相关移动端查询接口。
 */
@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
@Tag(name = "移动端工作台")
public class MobileController {

    private final MobileSamplingQueryService mobileSamplingQueryService;

    private final MobileDetectionQueryService mobileDetectionQueryService;

    private final MobileReviewQueryService mobileReviewQueryService;

    private final MobileReportQueryService mobileReportQueryService;

    private final AuthService authService;

    private final LabSecurityProperties securityProperties;

    /**
     * 获取移动端采样待办列表。
     *
     * @return 采样待办列表。
     */
    @GetMapping("/sampling/todo")
    @Operation(summary = "移动端采样待办")
    public ApiResponse<List<MobileSamplingTodoVO>> samplingTodo() {
        return ApiResponse.success(mobileSamplingQueryService.samplingTodo());
    }

    /**
     * 获取移动端检测待办列表。
     *
     * @return 检测待办列表。
     */
    @GetMapping("/detection/todo")
    @Operation(summary = "移动端检测待办")
    public ApiResponse<List<MobileDetectionTodoVO>> detectionTodo() {
        return ApiResponse.success(mobileDetectionQueryService.detectionTodo());
    }

    /**
     * 获取移动端检测历史列表。
     *
     * @return 检测历史列表。
     */
    @GetMapping("/detection/history")
    @Operation(summary = "移动端检测历史")
    public ApiResponse<List<MobileDetectionHistoryVO>> detectionHistory() {
        return ApiResponse.success(mobileDetectionQueryService.detectionHistory());
    }

    /**
     * 获取移动端审核历史列表。
     *
     * @return 审核历史列表。
     */
    @GetMapping("/review/history")
    @Operation(summary = "移动端审核历史")
    public ApiResponse<List<MobileReviewHistoryVO>> reviewHistory() {
        return ApiResponse.success(mobileReviewQueryService.reviewHistory());
    }

    /**
     * 获取移动端审核待办列表。
     *
     * @return 审核待办列表。
     */
    @GetMapping("/review/todo")
    @Operation(summary = "移动端审核待办")
    public ApiResponse<List<MobileReviewTodoVO>> reviewTodo() {
        return ApiResponse.success(mobileReviewQueryService.reviewTodo());
    }

    /**
     * 获取移动端我的报告列表。
     *
     * @return 报告列表。
     */
    @GetMapping("/reports/mine")
    @Operation(summary = "移动端我的报告")
    public ApiResponse<List<MobileReportVO>> reportMine() {
        return ApiResponse.success(mobileReportQueryService.reportMine());
    }

    /**
     * 获取移动端当前登录人资料。
     *
     * @return 当前登录人资料。
     */
    @GetMapping("/profile")
    @Operation(summary = "移动端当前登录人资料")
    public ApiResponse<UserProfileVO> profile() {
        return ApiResponse.success(authService.me());
    }

    /**
     * 修改移动端当前登录人资料。
     *
     * @param command 资料修改命令。
     * @param request HTTP 请求。
     * @return 修改后的当前登录人资料。
     */
    @PostMapping("/profile")
    @Operation(summary = "移动端修改当前登录人资料")
    public ApiResponse<UserProfileVO> updateProfile(@Valid @RequestBody UserProfileUpdateCommand command,
                                                    HttpServletRequest request) {
        return ApiResponse.success("资料修改成功", authService.updateProfile(command, resolveToken(request)));
    }

    /**
     * 修改移动端当前登录人密码。
     *
     * @param command 密码修改命令。
     * @return 操作结果。
     */
    @PostMapping("/changePassword")
    @Operation(summary = "移动端修改当前登录人密码")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeCommand command) {
        authService.changePassword(command);
        return ApiResponse.successMessage("密码修改成功");
    }

    /**
     * 移动端退出登录。
     *
     * @param request HTTP 请求。
     * @return 操作结果。
     */
    @PostMapping("/logout")
    @Operation(summary = "移动端退出登录")
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
