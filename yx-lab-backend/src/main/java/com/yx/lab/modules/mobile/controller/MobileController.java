package com.yx.lab.modules.mobile.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.modules.mobile.service.MobileReviewQueryService;
import com.yx.lab.modules.mobile.service.MobileSamplingQueryService;
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

    private final MobileReviewQueryService mobileReviewQueryService;

    @GetMapping("/sampling/todo")
    public ApiResponse<List<MobileSamplingTodoVO>> samplingTodo() {
        return ApiResponse.success(mobileSamplingQueryService.samplingTodo());
    }

    @GetMapping("/review/history")
    public ApiResponse<List<MobileReviewHistoryVO>> reviewHistory() {
        return ApiResponse.success(mobileReviewQueryService.reviewHistory());
    }

    @GetMapping("/review/todo")
    public ApiResponse<List<MobileReviewTodoVO>> reviewTodo() {
        return ApiResponse.success(mobileReviewQueryService.reviewTodo());
    }
}
