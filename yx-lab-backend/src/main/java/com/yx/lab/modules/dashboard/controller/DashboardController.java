package com.yx.lab.modules.dashboard.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.modules.dashboard.service.DashboardService;
import com.yx.lab.modules.dashboard.vo.DashboardOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 驾驶舱总览控制器。
 * 负责首页汇总指标查询。
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "驾驶舱总览")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取驾驶舱总览数据。
     *
     * @return 驾驶舱总览结果。
     */
    @GetMapping("/overview")
    @Operation(summary = "获取驾驶舱总览")
    public ApiResponse<DashboardOverviewVO> overview() {
        return ApiResponse.success(dashboardService.overview());
    }
}
