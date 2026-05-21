package com.yx.lab.modules.mobile.controller;

import com.yx.lab.common.config.LabSecurityProperties;
import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.modules.detection.dto.DetectionRecordQuery;
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
import com.yx.lab.modules.report.dto.ReportQuery;
import com.yx.lab.modules.review.dto.ReviewQuery;
import com.yx.lab.modules.sample.dto.LabSampleQuery;
import com.yx.lab.modules.sample.dto.SamplingTaskQuery;
import com.yx.lab.modules.system.dto.PasswordChangeCommand;
import com.yx.lab.modules.system.dto.UserProfileUpdateCommand;
import com.yx.lab.modules.system.service.AuthService;
import com.yx.lab.modules.system.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 移动端工作台控制器。
 * 统一提供采样、检测、审核、报告等移动端分页查询接口。
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
     * 分页查询移动端采样待办。
     *
     * @param query 复用 PC 端采样任务分页查询条件
     * @return 采样待办分页结果
     */
    @GetMapping("/sampling/todo")
    @Operation(summary = "分页查询移动端采样待办")
    public ApiResponse<PageResult<MobileSamplingTodoVO>> samplingTodo(@Validated SamplingTaskQuery query) {
        return ApiResponse.success(mobileSamplingQueryService.samplingTodo(query));
    }

    /**
     * 分页查询移动端检测待办。
     *
     * @param query 复用 PC 端样品分页查询条件
     * @return 检测待办分页结果
     */
    @GetMapping("/detection/todo")
    @Operation(summary = "分页查询移动端检测待办")
    public ApiResponse<PageResult<MobileDetectionTodoVO>> detectionTodo(@Validated LabSampleQuery query) {
        return ApiResponse.success(mobileDetectionQueryService.detectionTodo(query));
    }

    /**
     * 分页查询移动端检测历史。
     *
     * @param query 复用 PC 端检测流程分页查询条件
     * @return 检测历史分页结果
     */
    @GetMapping("/detection/history")
    @Operation(summary = "分页查询移动端检测历史")
    public ApiResponse<PageResult<MobileDetectionHistoryVO>> detectionHistory(@Validated DetectionRecordQuery query) {
        return ApiResponse.success(mobileDetectionQueryService.detectionHistory(query));
    }

    /**
     * 分页查询移动端审核历史。
     *
     * @param query 复用 PC 端审核记录分页查询条件
     * @return 审核历史分页结果
     */
    @GetMapping("/review/history")
    @Operation(summary = "分页查询移动端审核历史")
    public ApiResponse<PageResult<MobileReviewHistoryVO>> reviewHistory(@Validated ReviewQuery query) {
        return ApiResponse.success(mobileReviewQueryService.reviewHistory(query));
    }

    /**
     * 分页查询移动端审核待办。
     *
     * @param query 复用 PC 端检测流程分页查询条件
     * @return 审核待办分页结果
     */
    @GetMapping("/review/todo")
    @Operation(summary = "分页查询移动端审核待办")
    public ApiResponse<PageResult<MobileReviewTodoVO>> reviewTodo(@Validated DetectionRecordQuery query) {
        return ApiResponse.success(mobileReviewQueryService.reviewTodo(query));
    }

    /**
     * 分页查询移动端我的报告。
     *
     * @param query 复用 PC 端报告分页查询条件
     * @return 报告分页结果
     */
    @GetMapping("/reports/mine")
    @Operation(summary = "分页查询移动端我的报告")
    public ApiResponse<PageResult<MobileReportVO>> reportMine(@Validated ReportQuery query) {
        return ApiResponse.success(mobileReportQueryService.reportMine(query));
    }

    /**
     * 获取移动端当前登录人资料。
     *
     * @return 当前登录人资料
     */
    @GetMapping("/profile")
    @Operation(summary = "获取移动端当前登录人资料")
    public ApiResponse<UserProfileVO> profile() {
        return ApiResponse.success(authService.me());
    }

    /**
     * 修改移动端当前登录人资料。
     *
     * @param command 资料修改命令
     * @param request HTTP 请求
     * @return 修改后的当前登录人资料
     */
    @PostMapping("/profile")
    @Operation(summary = "修改移动端当前登录人资料")
    public ApiResponse<UserProfileVO> updateProfile(@Valid @RequestBody UserProfileUpdateCommand command,
                                                    HttpServletRequest request) {
        return ApiResponse.success("资料修改成功", authService.updateProfile(command, resolveToken(request)));
    }

    /**
     * 修改移动端当前登录人密码。
     *
     * @param command 密码修改命令
     * @return 操作结果
     */
    @PostMapping("/changePassword")
    @Operation(summary = "修改移动端当前登录人密码")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeCommand command) {
        authService.changePassword(command);
        return ApiResponse.successMessage("密码修改成功");
    }

    /**
     * 移动端退出登录。
     *
     * @param request HTTP 请求
     * @return 操作结果
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
