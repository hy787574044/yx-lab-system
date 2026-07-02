package com.yx.lab.modules.dashboard.service;

import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.modules.dashboard.vo.DashboardOverviewVO;
import com.yx.lab.modules.dashboard.vo.DetectorDashboardVO;
import com.yx.lab.modules.dashboard.vo.LeaderDashboardVO;
import com.yx.lab.modules.detection.entity.DetectionItem;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionItemMapper;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.statistics.vo.StatisticsDimensionItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页看板服务，负责组织统计摘要与快捷操作展示数据。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardQueryService dashboardQueryService;

    private final DetectionRecordMapper detectionRecordMapper;

    private final DetectionItemMapper detectionItemMapper;

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

    /**
     * 组装化验室主任首页数据。
     *
     * @return 主任首页管理视角数据
     */
    public LeaderDashboardVO leaderOverview() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime yearStart = today.withDayOfYear(1).atStartOfDay();

        LeaderDashboardVO vo = new LeaderDashboardVO();
        vo.setTopMetrics(buildTopMetrics(todayStart, monthStart, yearStart));
        vo.setProcessNodes(buildProcessNodes());
        vo.setQuickTodos(buildQuickTodos());
        vo.setPassRateTrend(Collections.emptyList());
        vo.setDetectorWorkloadRanking(Collections.emptyList());
        vo.setWarnings(Collections.emptyList());
        return vo;
    }

    /**
     * 组装检测员首页数据。
     *
     * @return 检测员工作台数据
     */
    public DetectorDashboardVO detectorOverview() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null) {
            return emptyDetectorDashboard();
        }
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime weekStart = today.minusDays(Math.max(today.getDayOfWeek().getValue() - 1, 0)).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        DetectorDashboardVO vo = new DetectorDashboardVO();
        vo.setRoleCode(currentUser.getRoleCode());
        vo.setStats(buildDetectorStats(currentUser, todayStart, weekStart, monthStart));
        vo.setTodoItems(buildDetectorTodoItems(currentUser));
        vo.setRecentRecords(buildDetectorRecentRecords(currentUser));
        return vo;
    }

    private DetectorDashboardVO emptyDetectorDashboard() {
        DetectorDashboardVO vo = new DetectorDashboardVO();
        DetectorDashboardVO.StatsVO stats = new DetectorDashboardVO.StatsVO();
        stats.setPendingCount(0L);
        stats.setTodayFinishCount(0L);
        stats.setWeekFinishCount(0L);
        stats.setMonthFinishCount(0L);
        stats.setAverageMinutes(0L);
        vo.setStats(stats);
        vo.setTodoItems(Collections.emptyList());
        vo.setRecentRecords(Collections.emptyList());
        return vo;
    }

    private DetectorDashboardVO.StatsVO buildDetectorStats(CurrentUser currentUser,
                                                           LocalDateTime todayStart,
                                                           LocalDateTime weekStart,
                                                           LocalDateTime monthStart) {
        Long userId = currentUser == null ? null : currentUser.getUserId();
        DetectorDashboardVO.StatsVO stats = new DetectorDashboardVO.StatsVO();
        stats.setPendingCount(countDetectionsByDetector(userId, null, LabWorkflowConstants.DetectionStatus.WAIT_DETECT));
        stats.setTodayFinishCount(countDetectionsByDetector(userId, todayStart, LabWorkflowConstants.DetectionStatus.SUBMITTED, LabWorkflowConstants.DetectionStatus.APPROVED));
        stats.setWeekFinishCount(countDetectionsByDetector(userId, weekStart, LabWorkflowConstants.DetectionStatus.SUBMITTED, LabWorkflowConstants.DetectionStatus.APPROVED));
        stats.setMonthFinishCount(countDetectionsByDetector(userId, monthStart, LabWorkflowConstants.DetectionStatus.SUBMITTED, LabWorkflowConstants.DetectionStatus.APPROVED));
        stats.setAverageMinutes(calcAverageMinutes(userId));
        return stats;
    }

    private List<DetectorDashboardVO.TodoItemVO> buildDetectorTodoItems(CurrentUser currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            return Collections.emptyList();
        }
        List<DetectionItem> items = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getDetectorId, currentUser.getUserId())
                .in(DetectionItem::getItemStatus,
                        LabWorkflowConstants.DetectionStatus.WAIT_DETECT,
                        LabWorkflowConstants.DetectionStatus.REJECTED)
                .orderByDesc(DetectionItem::getUpdatedTime)
                .orderByDesc(DetectionItem::getCreatedTime)
                .last("limit 8"));
        Map<Long, DetectionRecord> recordMap = loadRecordMap(items);
        return items.stream().map(item -> {
            DetectionRecord record = recordMap.get(item.getRecordId());
            DetectorDashboardVO.TodoItemVO vo = new DetectorDashboardVO.TodoItemVO();
            vo.setId(item.getId());
            vo.setRecordId(item.getRecordId());
            vo.setSampleNo(record == null ? null : record.getSampleNo());
            vo.setParameterName(item.getParameterName());
            vo.setMethodName(item.getMethodName());
            vo.setTitle((item.getParameterName() == null ? "" : item.getParameterName()) + " 待检测");
            vo.setMeta(buildTodoMeta(record, item));
            vo.setStatus(item.getItemStatus());
            vo.setStatusText(LabWorkflowConstants.getDetectionStatusLabel(item.getItemStatus()));
            return vo;
        }).collect(Collectors.toList());
    }

    private List<DetectorDashboardVO.RecentRecordVO> buildDetectorRecentRecords(CurrentUser currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            return Collections.emptyList();
        }
        List<DetectionItem> items = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getDetectorId, currentUser.getUserId())
                .in(DetectionItem::getItemStatus,
                        LabWorkflowConstants.DetectionStatus.SUBMITTED,
                        LabWorkflowConstants.DetectionStatus.APPROVED,
                        LabWorkflowConstants.DetectionStatus.REJECTED)
                .orderByDesc(DetectionItem::getUpdatedTime)
                .orderByDesc(DetectionItem::getCreatedTime)
                .last("limit 8"));
        Map<Long, DetectionRecord> recordMap = loadRecordMap(items);
        return items.stream().map(item -> {
            DetectionRecord record = recordMap.get(item.getRecordId());
            DetectorDashboardVO.RecentRecordVO vo = new DetectorDashboardVO.RecentRecordVO();
            vo.setId(item.getId());
            vo.setRecordId(item.getRecordId());
            vo.setSampleNo(record == null ? null : record.getSampleNo());
            vo.setParameterText(buildParameterText(item));
            vo.setResultValue(item.getResultValue() == null ? null : item.getResultValue().stripTrailingZeros().toPlainString());
            vo.setUnit(item.getUnit());
            vo.setResultText(LabWorkflowConstants.getDetectionResultLabel(resolveResultLabel(item)));
            vo.setStatus(item.getItemStatus());
            vo.setStatusText(LabWorkflowConstants.getDetectionStatusLabel(item.getItemStatus()));
            vo.setUpdatedTime(item.getUpdatedTime());
            return vo;
        }).collect(Collectors.toList());
    }

    private Map<Long, DetectionRecord> loadRecordMap(List<DetectionItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> recordIds = items.stream()
                .map(DetectionItem::getRecordId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (recordIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return detectionRecordMapper.selectList(new LambdaQueryWrapper<DetectionRecord>()
                        .in(DetectionRecord::getId, recordIds))
                .stream()
                .collect(Collectors.toMap(DetectionRecord::getId, record -> record, (left, right) -> left));
    }

    private String buildTodoMeta(DetectionRecord record, DetectionItem item) {
        List<String> parts = new ArrayList<>();
        if (record != null && record.getSampleNo() != null) {
            parts.add("样品" + record.getSampleNo());
        }
        if (item.getMethodName() != null) {
            parts.add(item.getMethodName());
        }
        if (item.getDetectorName() != null) {
            parts.add(item.getDetectorName());
        }
        return String.join(" · ", parts);
    }

    private String buildParameterText(DetectionItem item) {
        if (item == null) {
            return "";
        }
        String unit = item.getUnit() == null ? "" : item.getUnit();
        return item.getParameterName() + (unit.isEmpty() ? "" : " (" + unit + ")");
    }

    private String resolveResultLabel(DetectionItem item) {
        if (item == null) {
            return null;
        }
        return item.getExceedFlag() != null && item.getExceedFlag() == 1
                ? LabWorkflowConstants.DetectionResult.ABNORMAL
                : LabWorkflowConstants.DetectionResult.NORMAL;
    }

    private long countDetectionsByDetector(Long detectorId, LocalDateTime startTime, String... statuses) {
        if (detectorId == null || statuses == null || statuses.length == 0) {
            return 0L;
        }
        Long count = detectionItemMapper.selectCount(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getDetectorId, detectorId)
                .in(DetectionItem::getItemStatus, Arrays.asList(statuses))
                .ge(startTime != null, DetectionItem::getUpdatedTime, startTime));
        return count == null ? 0L : count.longValue();
    }

    private long calcAverageMinutes(Long detectorId) {
        if (detectorId == null) {
            return 0L;
        }
        List<DetectionItem> items = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getDetectorId, detectorId)
                .in(DetectionItem::getItemStatus,
                        LabWorkflowConstants.DetectionStatus.SUBMITTED,
                        LabWorkflowConstants.DetectionStatus.APPROVED)
                .orderByDesc(DetectionItem::getUpdatedTime)
                .last("limit 50"));
        if (items.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        long count = 0L;
        for (DetectionItem item : items) {
            if (item.getCreatedTime() == null || item.getUpdatedTime() == null) {
                continue;
            }
            long minutes = Math.max(0L, java.time.Duration.between(item.getCreatedTime(), item.getUpdatedTime()).toMinutes());
            total += minutes;
            count++;
        }
        return count == 0 ? 0L : total / count;
    }

    private Map<String, Long> buildResultSummary() {
        Map<String, Long> resultSummary = new LinkedHashMap<>();
        resultSummary.put("正常", dashboardQueryService.normalResultTotal());
        resultSummary.put("异常", dashboardQueryService.abnormalResultTotal());
        return resultSummary;
    }

    private List<String> buildQuickActions() {
        return Arrays.asList(
                "样品登录",
                "检测录入",
                "审核审批",
                "报告发布");
    }

    private List<LeaderDashboardVO.TopMetricVO> buildTopMetrics(LocalDateTime todayStart,
                                                                 LocalDateTime monthStart,
                                                                 LocalDateTime yearStart) {
        return Arrays.asList(
                topMetric("新增样品",
                        dashboardQueryService.sampleTotalFrom(todayStart),
                        dashboardQueryService.sampleTotalFrom(monthStart),
                        dashboardQueryService.sampleTotalFrom(yearStart),
                        null,
                        "新登录的样品总数",
                        "brand",
                        "/sample-ledger"),
                topMetric("完成检测",
                        dashboardQueryService.completedDetectionTotalFrom(todayStart),
                        dashboardQueryService.completedDetectionTotalFrom(monthStart),
                        dashboardQueryService.completedDetectionTotalFrom(yearStart),
                        null,
                        "已形成检测结果的样品数",
                        "success",
                        "/detection-ledger"),
                topMetric("进行中",
                        null,
                        null,
                        null,
                        dashboardQueryService.runningSampleTotal(),
                        "尚未完成闭环的样品数",
                        "warning",
                        "/detection-analysis"),
                topMetric("超标预警",
                        null,
                        null,
                        null,
                        dashboardQueryService.abnormalWarningTotal(),
                        "超标且仍需处理的数据",
                        "danger",
                        "/detection-ledger"));
    }

    private LeaderDashboardVO.TopMetricVO topMetric(String label,
                                                    Long todayValue,
                                                    Long monthValue,
                                                    Long yearValue,
                                                    Long realtimeValue,
                                                    String description,
                                                    String tone,
                                                    String path) {
        LeaderDashboardVO.TopMetricVO vo = new LeaderDashboardVO.TopMetricVO();
        vo.setLabel(label);
        vo.setTodayValue(todayValue);
        vo.setMonthValue(monthValue);
        vo.setYearValue(yearValue);
        vo.setRealtimeValue(realtimeValue);
        vo.setDescription(description);
        vo.setTone(tone);
        vo.setPath(path);
        return vo;
    }

    private List<LeaderDashboardVO.ProcessNodeVO> buildProcessNodes() {
        return Arrays.asList(
                processNode(1, "样品登录", dashboardQueryService.sampleStatusTotal(LabWorkflowConstants.SampleStatus.LOGGED), "待进入检测", "/sample-login"),
                processNode(2, "检测分样", dashboardQueryService.detectionStatusTotal(LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN), "待分配检测人员", "/detection-split"),
                processNode(3, "化验检测", dashboardQueryService.detectionStatusTotal(
                        LabWorkflowConstants.DetectionStatus.WAIT_DETECT,
                        LabWorkflowConstants.DetectionStatus.REJECTED), "待检测录入", "/detection-analysis"),
                processNode(4, "结果审查", dashboardQueryService.detectionStatusTotal(LabWorkflowConstants.DetectionStatus.SUBMITTED), "待审核确认", "/review-result"),
                processNode(5, "生成报告", dashboardQueryService.reportStatusTotal(LabWorkflowConstants.ReportStatus.DRAFT), "待生成正式报告", "/report-ledger"),
                processNode(6, "报告审查", dashboardQueryService.reportStatusTotal(LabWorkflowConstants.ReportStatus.GENERATED), "待发布审批", "/report-ledger"),
                processNode(7, "检测完成", dashboardQueryService.reportStatusTotal(LabWorkflowConstants.ReportStatus.PUBLISHED), "已发布闭环", "/report-ledger"));
    }

    private List<LeaderDashboardVO.QuickTodoVO> buildQuickTodos() {
        return Arrays.asList(
                quickTodo("sampling", "采样处理",
                        dashboardQueryService.samplingTaskStatusTotal(
                                LabWorkflowConstants.SamplingTaskStatus.PENDING,
                                LabWorkflowConstants.SamplingTaskStatus.IN_PROGRESS),
                        "待执行、执行中的采样任务",
                        "brand",
                        "/task-assign",
                        query("taskStatus", LabWorkflowConstants.SamplingTaskStatus.PENDING
                                + "," + LabWorkflowConstants.SamplingTaskStatus.IN_PROGRESS,
                                "autoOpen", "1")),
                quickTodo("sampleLogin", "样品登录",
                        dashboardQueryService.unregisteredCompletedSamplingTaskTotal(),
                        "已完成采样但尚未登录的样品",
                        "success",
                        "/sample-login",
                        query("taskStatus", LabWorkflowConstants.SamplingTaskStatus.COMPLETED,
                                "sampleRegisterStatus", LabWorkflowConstants.SampleRegisterStatus.UNREGISTERED,
                                "autoOpen", "1")),
                quickTodo("detection", "检测录入",
                        dashboardQueryService.detectionStatusTotal(
                                LabWorkflowConstants.DetectionStatus.WAIT_DETECT,
                                LabWorkflowConstants.DetectionStatus.REJECTED),
                        "待录入检测结果的检测流程",
                        "warning",
                        "/detection-analysis",
                        query("autoOpen", "1")),
                quickTodo("review", "结果审核",
                        dashboardQueryService.detectionStatusTotal(LabWorkflowConstants.DetectionStatus.SUBMITTED),
                        "待审核确认的检测结果",
                        "danger",
                        "/review-result",
                        query("reviewScope", "pending",
                                "autoOpen", "1")),
                quickTodo("report", "报告处理",
                        dashboardQueryService.reportStatusTotal(LabWorkflowConstants.ReportStatus.DRAFT)
                                + dashboardQueryService.reportStatusTotal(LabWorkflowConstants.ReportStatus.GENERATED),
                        "待生成或待发布的报告",
                        "info",
                        "/report-ledger",
                        query("reportStatus", LabWorkflowConstants.ReportStatus.DRAFT,
                                "autoOpen", "1"))
        );
    }

    private LeaderDashboardVO.QuickTodoVO quickTodo(String key,
                                                    String label,
                                                    Long count,
                                                    String description,
                                                    String tone,
                                                    String path,
                                                    Map<String, String> query) {
        LeaderDashboardVO.QuickTodoVO vo = new LeaderDashboardVO.QuickTodoVO();
        vo.setKey(key);
        vo.setLabel(label);
        vo.setCount(count == null ? 0L : count);
        vo.setDescription(description);
        vo.setTone(tone);
        vo.setPath(path);
        vo.setQuery(query);
        return vo;
    }

    private Map<String, String> query(String... entries) {
        Map<String, String> query = new LinkedHashMap<>();
        if (entries == null) {
            return query;
        }
        for (int index = 0; index + 1 < entries.length; index += 2) {
            query.put(entries[index], entries[index + 1]);
        }
        return query;
    }

    private LeaderDashboardVO.ProcessNodeVO processNode(Integer index,
                                                        String label,
                                                        Long value,
                                                        String statusText,
                                                        String path) {
        LeaderDashboardVO.ProcessNodeVO vo = new LeaderDashboardVO.ProcessNodeVO();
        vo.setIndex(index);
        vo.setLabel(label);
        vo.setValue(value);
        vo.setStatusText(statusText);
        vo.setWarning(value != null && value > 0 && index >= 2 && index <= 6);
        vo.setPath(path);
        return vo;
    }

    private List<LeaderDashboardVO.TrendItemVO> buildPassRateTrend(LocalDate today) {
        List<LeaderDashboardVO.TrendItemVO> rows = new ArrayList<>();
        for (int index = 6; index >= 0; index--) {
            LocalDate date = today.minusDays(index);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            long normal = dashboardQueryService.detectionResultTotalBetween(LabWorkflowConstants.DetectionResult.NORMAL, start, end);
            long abnormal = dashboardQueryService.detectionResultTotalBetween(LabWorkflowConstants.DetectionResult.ABNORMAL, start, end);
            LeaderDashboardVO.TrendItemVO vo = new LeaderDashboardVO.TrendItemVO();
            vo.setLabel(date.getMonthValue() + "/" + date.getDayOfMonth());
            vo.setValue(rate(normal, normal + abnormal));
            vo.setNormalCount(normal);
            vo.setAbnormalCount(abnormal);
            vo.setTarget(BigDecimal.valueOf(95));
            rows.add(vo);
        }
        return rows;
    }

    private List<LeaderDashboardVO.RankingItemVO> buildDetectorWorkloadRanking(LocalDateTime monthStart) {
        return dashboardQueryService.detectorWorkloadRanking(monthStart).stream()
                .map(this::rankingItem)
                .collect(Collectors.toList());
    }

    private LeaderDashboardVO.RankingItemVO rankingItem(StatisticsDimensionItemVO item) {
        LeaderDashboardVO.RankingItemVO vo = new LeaderDashboardVO.RankingItemVO();
        vo.setName(item.getName());
        vo.setValue(item.getValue());
        return vo;
    }

    private List<LeaderDashboardVO.WarningItemVO> buildWarnings() {
        LocalDateTime now = LocalDateTime.now();
        return Arrays.asList(
                warningItem("待审核超时",
                        "检测完成超过24小时仍未审核",
                        "建议优先进入结果审查页面处理超时记录",
                        "高",
                        dashboardQueryService.pendingReviewTimeoutTotal(now.minusHours(24)),
                        "/review-result"),
                warningItem("超标未处理",
                        "超标数据超过2小时未复核",
                        "建议回溯异常样品并完成复核或重检",
                        "高",
                        dashboardQueryService.abnormalUnhandledTotal(now.minusHours(2)),
                        "/detection-ledger"),
                warningItem("仪器即将过期",
                        "仪器检定/校准有效期即将到期",
                        "当前仪器到期字段暂未接入，后续可接入设备台账有效期",
                        "中",
                        0L,
                        "/instrument-ledger"),
                warningItem("质控样失效",
                        "标准物质或质控样即将失效",
                        "当前质控样有效期暂未接入，后续可接入耗材或质控台账",
                        "中",
                        0L,
                        "/statistics-quality"));
    }

    private LeaderDashboardVO.WarningItemVO warningItem(String type,
                                                        String title,
                                                        String content,
                                                        String priority,
                                                        Long count,
                                                        String path) {
        LeaderDashboardVO.WarningItemVO vo = new LeaderDashboardVO.WarningItemVO();
        vo.setType(type);
        vo.setTitle(title);
        vo.setContent(content);
        vo.setPriority(priority);
        vo.setCount(count);
        vo.setPath(path);
        return vo;
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}
