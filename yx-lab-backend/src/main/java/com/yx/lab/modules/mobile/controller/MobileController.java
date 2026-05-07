package com.yx.lab.modules.mobile.controller;

import com.yx.lab.common.model.ApiResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
