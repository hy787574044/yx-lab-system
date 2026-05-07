package com.yx.lab.modules.statistics.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.modules.statistics.service.StatisticsService;
import com.yx.lab.modules.statistics.vo.StatisticsSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "统计分析")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/summary")
    @Operation(summary = "获取统计汇总")
    public ApiResponse<StatisticsSummaryVO> summary() {
        return ApiResponse.success(statisticsService.summary());
    }
}
