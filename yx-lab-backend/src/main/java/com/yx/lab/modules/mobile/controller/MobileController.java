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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
public class MobileController {

    private final MobileSamplingQueryService mobileSamplingQueryService;

    private final MobileDetectionQueryService mobileDetectionQueryService;

    private final MobileReviewQueryService mobileReviewQueryService;

    private final MobileReportQueryService mobileReportQueryService;

    @GetMapping("/sampling/todo")
    public ApiResponse<List<MobileSamplingTodoVO>> samplingTodo() {
        return ApiResponse.success(mobileSamplingQueryService.samplingTodo());
    }

    @GetMapping("/detection/todo")
    public ApiResponse<List<MobileDetectionTodoVO>> detectionTodo() {
        return ApiResponse.success(mobileDetectionQueryService.detectionTodo());
    }

    @GetMapping("/detection/history")
    public ApiResponse<List<MobileDetectionHistoryVO>> detectionHistory() {
        return ApiResponse.success(mobileDetectionQueryService.detectionHistory());
    }

    @GetMapping("/review/history")
    public ApiResponse<List<MobileReviewHistoryVO>> reviewHistory() {
        return ApiResponse.success(mobileReviewQueryService.reviewHistory());
    }

    @GetMapping("/review/todo")
    public ApiResponse<List<MobileReviewTodoVO>> reviewTodo() {
        return ApiResponse.success(mobileReviewQueryService.reviewTodo());
    }

    @GetMapping("/reports/mine")
    public ApiResponse<List<MobileReportVO>> reportMine() {
        return ApiResponse.success(mobileReportQueryService.reportMine());
    }
}
