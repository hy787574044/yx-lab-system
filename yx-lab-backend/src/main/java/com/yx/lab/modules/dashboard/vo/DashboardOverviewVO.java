package com.yx.lab.modules.dashboard.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DashboardOverviewVO {

    private Long sampleTotal;

    private Long pendingReviewTotal;

    private Long approvedTotal;

    private Long publishedReportTotal;

    private Map<String, Long> resultSummary;

    private List<String> quickActions;
}
