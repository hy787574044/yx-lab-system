package com.yx.lab.modules.dashboard.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.security.PermissionConstants;
import com.yx.lab.common.security.RequirePermission;
import com.yx.lab.modules.dashboard.service.DashboardService;
import com.yx.lab.modules.dashboard.vo.DashboardOverviewVO;
import com.yx.lab.modules.dashboard.vo.DetectorDashboardVO;
import com.yx.lab.modules.dashboard.vo.LeaderDashboardVO;
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
@RequirePermission(PermissionConstants.DASHBOARD_VIEW)
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

    /**
     * 获取主任首页管理视角数据。
     *
     * @return 主任首页管理视角数据。
     */
    @GetMapping("/leaderOverview")
    @Operation(summary = "获取主任首页管理视角")
    public ApiResponse<LeaderDashboardVO> leaderOverview() {
        return ApiResponse.success(dashboardService.leaderOverview());
    }

    /**
     * 获取检测员首页工作台数据。
     *
     * @return 检测员首页工作台数据。
     */
    @GetMapping("/detectorOverview")
    @Operation(summary = "获取检测员首页工作台")
    public ApiResponse<DetectorDashboardVO> detectorOverview() {
        return ApiResponse.success(dashboardService.detectorOverview());
    }
}
