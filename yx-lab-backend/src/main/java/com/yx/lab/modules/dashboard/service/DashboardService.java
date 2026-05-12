package com.yx.lab.modules.dashboard.service;

import com.yx.lab.modules.dashboard.vo.DashboardOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页看板服务，负责组织统计摘要与快捷操作展示数据。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardQueryService dashboardQueryService;

    /**
     * 组装首页看板总览数据。
     *
     * @return 首页看板总览
     */
    public DashboardOverviewVO overview() {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        vo.setSampleTotal(dashboardQueryService.sampleTotal());
        vo.setPendingReviewTotal(dashboardQueryService.pendingReviewTotal());
        vo.setApprovedTotal(dashboardQueryService.approvedDetectionTotal());
        vo.setPublishedReportTotal(dashboardQueryService.publishedReportTotal());
        vo.setResultSummary(buildResultSummary());
        vo.setQuickActions(buildQuickActions());
        return vo;
    }

    private Map<String, Long> buildResultSummary() {
        Map<String, Long> resultSummary = new LinkedHashMap<>();
        resultSummary.put("\u6b63\u5e38", dashboardQueryService.normalResultTotal());
        resultSummary.put("\u5f02\u5e38", dashboardQueryService.abnormalResultTotal());
        return resultSummary;
    }

    private List<String> buildQuickActions() {
        return Arrays.asList(
                "\u6837\u54c1\u767b\u5f55",
                "\u68c0\u6d4b\u5f55\u5165",
                "\u5ba1\u6838\u5ba1\u6279",
                "\u62a5\u544a\u53d1\u5e03");
    }
}
