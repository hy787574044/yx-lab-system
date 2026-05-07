package com.yx.lab.modules.mobile.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.modules.mobile.service.MobileService;
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

    private final MobileService mobileService;

    @GetMapping("/sampling/todo")
    public ApiResponse<List<MobileSamplingTodoVO>> samplingTodo() {
        return ApiResponse.success(mobileService.samplingTodo());
    }

    @GetMapping("/review/history")
    public ApiResponse<List<MobileReviewHistoryVO>> reviewHistory() {
        return ApiResponse.success(mobileService.reviewHistory());
    }

    @GetMapping("/review/todo")
    public ApiResponse<List<MobileReviewTodoVO>> reviewTodo() {
        return ApiResponse.success(mobileService.reviewTodo());
    }
}
