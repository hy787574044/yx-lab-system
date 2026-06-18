package com.yx.lab.modules.report.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.DataScopeHelper;
import com.yx.lab.common.util.ExcelExportUtil;
import com.yx.lab.modules.detection.entity.DetectionItem;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionItemMapper;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.report.dto.SummaryReportPreviewQuery;
import com.yx.lab.modules.report.dto.SummaryReportQuery;
import com.yx.lab.modules.report.vo.SummaryReportListVO;
import com.yx.lab.modules.report.vo.SummaryReportPreviewRowVO;
import com.yx.lab.modules.report.vo.SummaryReportPreviewVO;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.entity.MonitoringPoint;
import com.yx.lab.modules.sample.entity.SamplingPlan;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.sample.mapper.MonitoringPointMapper;
import com.yx.lab.modules.sample.mapper.SamplingPlanMapper;
import com.yx.lab.modules.sample.mapper.SamplingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummaryReportService {

    public static final String SUMMARY_TYPE_DAILY = "DAILY";
    public static final String SUMMARY_TYPE_WEEKLY = "WEEKLY";
    public static final String SUMMARY_TYPE_HALF_MONTHLY = "HALF_MONTHLY";
    public static final String DAILY_REPORT_TYPE_INTERNAL = "INTERNAL";
    public static final String DAILY_REPORT_TYPE_EXTERNAL = "EXTERNAL";

    private static final String REPORT_STATUS_PENDING = "PENDING";
    private static final String REPORT_STATUS_PARTIAL = "PARTIAL";
    private static final String REPORT_STATUS_COMPLETE = "COMPLETE";
    private static final String PREVIEW_MODE_GENERIC = "GENERIC";
    private static final String PREVIEW_MODE_DAILY_INTERNAL = "DAILY_INTERNAL_TEMPLATE";
    private static final String PREVIEW_MODE_DAILY_EXTERNAL = "DAILY_EXTERNAL_TEMPLATE";
    private static final String PREVIEW_MODE_WEEKLY_FACTORY = "WEEKLY_FACTORY_TEMPLATE";
    private static final String PREVIEW_MODE_HALF_MONTHLY_TERMINAL = "HALF_MONTHLY_TERMINAL_TEMPLATE";
    private static final String REPORT_UNIT_NAME = "阳新县城发水务有限公司";
    private static final List<String> INTERNAL_TIME_BUCKETS = Arrays.asList("08:00", "10:00", "12:00", "14:00", "16:00");
    private static final List<String> DAILY_INTERNAL_COLUMNS = Arrays.asList(
            "水源水-水温℃", "水源水-PH值", "水源水-浑浊度NTU", "水源水-氨氮(mg/L)",
            "滤前水-浑浊度NTU",
            "出厂水-菌落总数CFU/ml", "出厂水-总大肠菌群MPN/100ml", "出厂水-大肠埃希氏菌CFU/ml",
            "出厂水-浑浊度NTU", "出厂水-PH值", "出厂水-色度(度)", "出厂水-臭和味",
            "出厂水-肉眼可见物", "出厂水-余氯(mg/L)", "出厂水-高锰酸盐指数(mg/L)"
    );
    private static final List<String> DAILY_EXTERNAL_COLUMNS = Arrays.asList(
            "浑浊度(NTU)", "色度(度)", "臭和味", "肉眼可见物", "游离余氯(mg/L)"
    );
    private static final List<String> DAILY_EXTERNAL_SAMPLE_TYPES = Arrays.asList(
            LabWorkflowConstants.SampleType.FACTORY,
            LabWorkflowConstants.SampleType.TERMINAL
    );
    private static final List<String> WEEKLY_FACTORY_COLUMNS = Arrays.asList(
            "色度(度)", "浑浊度(NTU)", "臭和味", "肉眼可见物", "余氯(mg/L)",
            "菌落总数(CFU/ml)", "总大肠菌群(MPN/100ml)", "大肠埃希氏菌(CFU/ml)", "高锰酸盐指数(mg/L)", "备注"
    );
    private static final List<String> HALF_MONTHLY_TERMINAL_COLUMNS = Arrays.asList(
            "色度(度)", "浑浊度(NTU)", "PH", "臭和味", "游离氯(mg/L)",
            "菌落总数(CFU/ml)", "总大肠菌群(MPN/100ml)", "高锰酸盐指数(mg/L)"
    );

    private static final String DEFAULT_REGION_NAME = "未配置水厂";

    private static final List<String> PREFERRED_PARAMETER_ORDER = java.util.Arrays.asList(
            "水温", "水温℃", "PH值", "pH", "浊度", "浮浊度", "色度", "臭和味", "肉眼可见物",
            "余氯", "游离余氯", "菌落总数", "总大肠菌群", "大肠埃希氏菌", "高锰酸盐指数", "氨氮");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final SamplingTaskMapper samplingTaskMapper;

    private final LabSampleMapper labSampleMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    private final DetectionItemMapper detectionItemMapper;

    private final MonitoringPointMapper monitoringPointMapper;

    private final SamplingPlanMapper samplingPlanMapper;

    private final DataScopeHelper dataScopeHelper;

    public PageResult<SummaryReportListVO> page(SummaryReportQuery query) {
        validateSummaryType(query == null ? null : query.getSummaryType());
        validateSummaryAccess(query == null ? null : query.getSummaryType());
        validateDailyReportType(query == null ? null : query.getSummaryType(), query == null ? null : query.getDailyReportType(), false);
        List<SummaryTaskBundle> bundles = loadBundles(query);
        List<SummaryReportListVO> reports = buildSummaryList(
                query == null ? null : query.getSummaryType(),
                query == null ? null : query.getDailyReportType(),
                bundles);
        String keyword = query == null ? null : StrUtil.trim(query.getKeyword());
        if (StrUtil.isNotBlank(keyword)) {
            reports = reports.stream()
                    .filter(item -> containsText(item.getReportName(), keyword)
                            || containsText(item.getRegionName(), keyword)
                            || containsText(item.getPeriodLabel(), keyword))
                    .collect(Collectors.toList());
        }
        String reportStatus = query == null ? null : StrUtil.trim(query.getReportStatus());
        if (StrUtil.isNotBlank(reportStatus)) {
            reports = reports.stream()
                    .filter(item -> StrUtil.equals(reportStatus, item.getReportStatus()))
                    .collect(Collectors.toList());
        }
        reports.sort(Comparator
                .comparing(SummaryReportListVO::getPeriodStart, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(SummaryReportListVO::getRegionName, Comparator.nullsLast(String::compareTo)));
        long total = reports.size();
        List<SummaryReportListVO> pageRecords = slicePage(reports, query == null ? 1L : query.getPageNum(), query == null ? 10L : query.getPageSize());
        return new PageResult<>(total, pageRecords);
    }

    public SummaryReportPreviewVO preview(SummaryReportPreviewQuery query) {
        validateSummaryType(query == null ? null : query.getSummaryType());
        validateSummaryAccess(query == null ? null : query.getSummaryType());
        validateDailyReportType(query == null ? null : query.getSummaryType(), query == null ? null : query.getDailyReportType(), true);
        if (query == null || query.getPeriodStart() == null || query.getPeriodEnd() == null) {
            throw new BusinessException("请选择需要预览的报表周期");
        }
        List<SummaryTaskBundle> bundles = loadBundlesForPreview(query);
        return buildPreview(
                query.getSummaryType(),
                query.getDailyReportType(),
                query.getRegionName(),
                query.getPeriodStart(),
                query.getPeriodEnd(),
                bundles);
    }

    public ResponseEntity<byte[]> exportList(SummaryReportQuery query) {
        if (query == null) {
            query = new SummaryReportQuery();
        }
        query.setPageNum(1L);
        query.setPageSize(10000L);
        List<SummaryReportListVO> reports = page(query).getRecords();
        String summaryLabel = getSummaryTypeLabel(query.getSummaryType());
        return ExcelExportUtil.buildResponse(
                summaryLabel + "列表.xlsx",
                summaryLabel + "列表",
                reports,
                java.util.Arrays.asList(
                        ExcelExportUtil.column("报表名称", SummaryReportListVO::getReportName),
                        ExcelExportUtil.column("所属水厂", SummaryReportListVO::getRegionName),
                        ExcelExportUtil.column("周期范围", SummaryReportListVO::getPeriodLabel),
                        ExcelExportUtil.column("监测点数", SummaryReportListVO::getPointCount),
                        ExcelExportUtil.column("任务数", SummaryReportListVO::getTaskCount),
                        ExcelExportUtil.column("样品数", SummaryReportListVO::getSampleCount),
                        ExcelExportUtil.column("已出结果任务数", SummaryReportListVO::getDetectedTaskCount),
                        ExcelExportUtil.column("结果明细数", SummaryReportListVO::getResultItemCount),
                        ExcelExportUtil.column("报表状态", SummaryReportListVO::getReportStatusLabel),
                        ExcelExportUtil.column("最新采样时间", SummaryReportListVO::getLatestSamplingTime),
                        ExcelExportUtil.column("最新检测时间", SummaryReportListVO::getLatestDetectionTime)
                ));
    }

    public ResponseEntity<byte[]> exportDetail(SummaryReportPreviewQuery query) {
        SummaryReportPreviewVO preview = preview(query);
        List<ExcelExportUtil.ExcelColumn<SummaryReportPreviewRowVO>> columns = new ArrayList<>();
        if (isInternalRecordTemplate(preview.getPreviewMode())) {
            columns.add(ExcelExportUtil.column("时间段", SummaryReportPreviewRowVO::getTimeBucket));
        }
        columns.add(ExcelExportUtil.column("采样时间", SummaryReportPreviewRowVO::getSamplingTimeLabel));
        columns.add(ExcelExportUtil.column("采样地点", SummaryReportPreviewRowVO::getPointName));
        columns.add(ExcelExportUtil.column("水样类型", SummaryReportPreviewRowVO::getSampleTypeLabel));
        columns.add(ExcelExportUtil.column("样品编号", SummaryReportPreviewRowVO::getSampleNo));
        columns.add(ExcelExportUtil.column("来源任务数", SummaryReportPreviewRowVO::getSourceTaskCount));
        for (String parameterName : preview.getParameterColumns()) {
            columns.add(ExcelExportUtil.column(parameterName,
                    row -> row.getParameterValues().get(parameterName)));
        }
        String fileName = sanitizeFileName(StrUtil.blankToDefault(preview.getReportName(), getSummaryTypeLabel(query.getSummaryType()))) + ".xlsx";
        return ExcelExportUtil.buildResponse(fileName,
                StrUtil.blankToDefault(preview.getSummaryTypeLabel(), "汇总报表"),
                preview.getRows(),
                columns);
    }

    private List<SummaryTaskBundle> loadBundles(SummaryReportQuery query) {
        LocalDateTime from = resolveQueryDateFrom(query == null ? null : query.getSummaryType(), query == null ? null : query.getDateFrom());
        LocalDateTime to = resolveQueryDateTo(query == null ? null : query.getSummaryType(), query == null ? null : query.getDateTo());
        LambdaQueryWrapper<SamplingTask> wrapper = new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getTaskStatus, LabWorkflowConstants.SamplingTaskStatus.COMPLETED)
                .isNotNull(SamplingTask::getSamplingTime)
                .ge(from != null, SamplingTask::getSamplingTime, from)
                .le(to != null, SamplingTask::getSamplingTime, to)
                .orderByDesc(SamplingTask::getSamplingTime)
                .orderByDesc(SamplingTask::getCreatedTime);
        applyTaskSamplerScope(wrapper);
        return assembleBundles(
                samplingTaskMapper.selectList(wrapper),
                query == null ? null : query.getRegionName(),
                query == null ? null : query.getPointName(),
                query == null ? null : query.getSummaryType(),
                null,
                null);
    }

    private List<SummaryTaskBundle> loadBundlesForPreview(SummaryReportPreviewQuery query) {
        LambdaQueryWrapper<SamplingTask> wrapper = new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getTaskStatus, LabWorkflowConstants.SamplingTaskStatus.COMPLETED)
                .isNotNull(SamplingTask::getSamplingTime)
                .ge(SamplingTask::getSamplingTime, query.getPeriodStart().atStartOfDay())
                .le(SamplingTask::getSamplingTime, query.getPeriodEnd().atTime(LocalTime.MAX))
                .orderByAsc(SamplingTask::getSamplingTime)
                .orderByAsc(SamplingTask::getCreatedTime);
        applyTaskSamplerScope(wrapper);
        return assembleBundles(
                samplingTaskMapper.selectList(wrapper),
                query.getRegionName(),
                query.getPointName(),
                query.getSummaryType(),
                query.getPeriodStart(),
                query.getPeriodEnd());
    }

    private List<SummaryTaskBundle> assembleBundles(List<SamplingTask> tasks,
                                                    String regionName,
                                                    String pointName,
                                                    String summaryType,
                                                    LocalDate expectedStart,
                                                    LocalDate expectedEnd) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, MonitoringPoint> pointMap = loadMonitoringPointMap(tasks);
        Map<Long, SamplingPlan> planMap = loadSamplingPlanMap(tasks);
        List<SamplingTask> filteredTasks = tasks.stream()
                .filter(task -> filterTaskByRegion(task, pointMap.get(task.getPointId()), regionName))
                .filter(task -> filterTaskByPoint(task, pointName))
                .filter(task -> filterTaskBySummaryPlanCycle(task, planMap.get(task.getPlanId()), summaryType))
                .filter(task -> filterTaskByExpectedPeriod(task, summaryType, expectedStart, expectedEnd))
                .filter(task -> filterTaskByWeeklyFixedTime(task, summaryType))
                .collect(Collectors.toList());
        if (filteredTasks.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, LabSample> sampleMap = loadSampleMap(filteredTasks);
        Map<Long, DetectionRecord> recordMap = loadDetectionRecordMap(sampleMap.values());
        Map<Long, List<DetectionItem>> itemMap = loadDetectionItemMap(recordMap.values());
        List<SummaryTaskBundle> bundles = new ArrayList<>();
        for (SamplingTask task : filteredTasks) {
            MonitoringPoint point = pointMap.get(task.getPointId());
            LabSample sample = sampleMap.get(task.getId());
            DetectionRecord record = sample == null ? null : recordMap.get(sample.getId());
            List<DetectionItem> items = record == null ? Collections.emptyList() : itemMap.getOrDefault(record.getId(), Collections.emptyList());
            bundles.add(buildBundle(task, planMap.get(task.getPlanId()), point, sample, record, items));
        }
        bundles.sort(Comparator
                .comparing((SummaryTaskBundle item) -> item.task.getSamplingTime(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(item -> safeText(item.task.getPointName()))
                .thenComparing(item -> safeText(item.task.getTaskNo())));
        return bundles;
    }

    private Map<Long, MonitoringPoint> loadMonitoringPointMap(List<SamplingTask> tasks) {
        List<Long> pointIds = tasks.stream()
                .map(SamplingTask::getPointId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (pointIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return monitoringPointMapper.selectList(new LambdaQueryWrapper<MonitoringPoint>()
                        .in(MonitoringPoint::getId, pointIds))
                .stream()
                .collect(Collectors.toMap(MonitoringPoint::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, SamplingPlan> loadSamplingPlanMap(List<SamplingTask> tasks) {
        List<Long> planIds = tasks.stream()
                .map(SamplingTask::getPlanId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (planIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return samplingPlanMapper.selectList(new LambdaQueryWrapper<SamplingPlan>()
                        .in(SamplingPlan::getId, planIds))
                .stream()
                .collect(Collectors.toMap(SamplingPlan::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, LabSample> loadSampleMap(List<SamplingTask> tasks) {
        List<Long> taskIds = tasks.stream()
                .map(SamplingTask::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<LabSample> samples = labSampleMapper.selectList(new LambdaQueryWrapper<LabSample>()
                .in(LabSample::getTaskId, taskIds));
        Map<Long, LabSample> result = new LinkedHashMap<>();
        for (LabSample sample : samples) {
            if (sample.getTaskId() == null) {
                continue;
            }
            LabSample existing = result.get(sample.getTaskId());
            if (existing == null || compareSample(sample, existing) > 0) {
                result.put(sample.getTaskId(), sample);
            }
        }
        return result;
    }

    private Map<Long, DetectionRecord> loadDetectionRecordMap(java.util.Collection<LabSample> samples) {
        List<Long> sampleIds = samples.stream()
                .map(LabSample::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (sampleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DetectionRecord> records = detectionRecordMapper.selectList(new LambdaQueryWrapper<DetectionRecord>()
                .in(DetectionRecord::getSampleId, sampleIds));
        Map<Long, DetectionRecord> result = new LinkedHashMap<>();
        for (DetectionRecord record : records) {
            if (record.getSampleId() == null) {
                continue;
            }
            DetectionRecord existing = result.get(record.getSampleId());
            if (existing == null || compareRecord(record, existing) > 0) {
                result.put(record.getSampleId(), record);
            }
        }
        return result;
    }

    private Map<Long, List<DetectionItem>> loadDetectionItemMap(java.util.Collection<DetectionRecord> records) {
        List<Long> recordIds = records.stream()
                .map(DetectionRecord::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (recordIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                        .in(DetectionItem::getRecordId, recordIds)
                        .orderByAsc(DetectionItem::getCreatedTime))
                .stream()
                .collect(Collectors.groupingBy(DetectionItem::getRecordId, LinkedHashMap::new, Collectors.toList()));
    }

    private SummaryTaskBundle buildBundle(SamplingTask task,
                                          SamplingPlan plan,
                                          MonitoringPoint point,
                                          LabSample sample,
                                          DetectionRecord record,
                                          List<DetectionItem> items) {
        SummaryTaskBundle bundle = new SummaryTaskBundle();
        bundle.task = task;
        bundle.plan = plan;
        bundle.point = point;
        bundle.sample = sample;
        bundle.record = record;
        bundle.items = items == null ? Collections.emptyList() : items;
        bundle.regionName = resolveRegionName(point);
        bundle.expectedParameterNames = parseExpectedParameters(task.getDetectionItems());
        bundle.parameterValueMap = new LinkedHashMap<>();
        bundle.approvedParameterValueMap = new LinkedHashMap<>();
        for (DetectionItem item : bundle.items) {
            String parameterName = StrUtil.trim(item.getParameterName());
            if (StrUtil.isBlank(parameterName)) {
                continue;
            }
            String valueText = formatResultValue(item.getResultValue(), item.getUnit());
            if (StrUtil.isBlank(valueText)) {
                continue;
            }
            bundle.parameterValueMap.put(parameterName, valueText);
            if (LabWorkflowConstants.DetectionStatus.APPROVED.equals(item.getItemStatus())) {
                bundle.approvedParameterValueMap.put(parameterName, valueText);
            }
        }
        bundle.filledResultCount = (int) bundle.parameterValueMap.values().stream()
                .filter(StrUtil::isNotBlank)
                .count();
        return bundle;
    }

    private List<SummaryReportListVO> buildSummaryList(String summaryType,
                                                       String dailyReportType,
                                                       List<SummaryTaskBundle> bundles) {
        String normalizedDailyReportType = normalizeDailyReportType(dailyReportType);
        Map<String, SummaryAccumulator> grouped = new LinkedHashMap<>();
        for (SummaryTaskBundle bundle : bundles) {
            SummaryPeriod period = resolveSummaryPeriod(summaryType, bundle.task.getSamplingTime());
            String key = buildReportKey(summaryType, bundle.regionName, period.start, period.end);
            SummaryAccumulator accumulator = grouped.computeIfAbsent(key,
                    unused -> new SummaryAccumulator(summaryType, bundle.regionName, period));
            accumulator.add(bundle);
        }
        List<SummaryReportListVO> result = new ArrayList<>();
        for (SummaryAccumulator accumulator : grouped.values()) {
            if (SUMMARY_TYPE_DAILY.equals(summaryType)) {
                if (StrUtil.isBlank(normalizedDailyReportType) || DAILY_REPORT_TYPE_INTERNAL.equals(normalizedDailyReportType)) {
                    result.add(accumulator.toListVO(DAILY_REPORT_TYPE_INTERNAL));
                }
                if (StrUtil.isBlank(normalizedDailyReportType) || DAILY_REPORT_TYPE_EXTERNAL.equals(normalizedDailyReportType)) {
                    result.add(accumulator.toListVO(DAILY_REPORT_TYPE_EXTERNAL));
                }
                continue;
            }
            result.add(accumulator.toListVO(null));
        }
        return result;
    }

    private SummaryReportPreviewVO buildPreview(String summaryType,
                                                String dailyReportType,
                                                String regionName,
                                                LocalDate periodStart,
                                                LocalDate periodEnd,
                                                List<SummaryTaskBundle> bundles) {
        SummaryReportPreviewVO preview = new SummaryReportPreviewVO();
        preview.setSummaryType(summaryType);
        preview.setSummaryTypeLabel(getSummaryTypeLabel(summaryType));
        preview.setDailyReportType(normalizeDailyReportType(dailyReportType));
        preview.setDailyReportTypeLabel(getDailyReportTypeLabel(dailyReportType));
        preview.setRegionName(StrUtil.blankToDefault(StrUtil.trim(regionName), DEFAULT_REGION_NAME));
        preview.setPeriodStart(periodStart);
        preview.setPeriodEnd(periodEnd);
        preview.setPeriodLabel(buildPeriodLabel(periodStart, periodEnd));
        preview.setReportUnit(REPORT_UNIT_NAME);
        preview.setWeekdayLabel(resolveWeekdayLabel(periodStart));
        preview.setWeather(resolveWeather(bundles));
        preview.setInspectorName(resolveInspectorName(bundles));
        preview.setReporterName(resolveReporterName(bundles));
        preview.setPrincipalName("");
        preview.setPreviewMode(PREVIEW_MODE_GENERIC);
        preview.setReportName(buildReportName(summaryType, preview.getRegionName(), preview.getPeriodLabel(), preview.getDailyReportType()));

        SummaryAccumulator accumulator = new SummaryAccumulator(summaryType, preview.getRegionName(), new SummaryPeriod(periodStart, periodEnd));
        bundles.forEach(accumulator::add);
        applyAccumulator(preview, accumulator);

        if (SUMMARY_TYPE_DAILY.equals(summaryType)) {
            if (DAILY_REPORT_TYPE_INTERNAL.equals(preview.getDailyReportType())) {
                preview.setPreviewMode(PREVIEW_MODE_DAILY_INTERNAL);
                preview.setParameterColumns(new ArrayList<>(DAILY_INTERNAL_COLUMNS));
                preview.setRows(buildDailyInternalRows(bundles));
                return preview;
            }
            preview.setPreviewMode(PREVIEW_MODE_DAILY_EXTERNAL);
            preview.setParameterColumns(new ArrayList<>(DAILY_EXTERNAL_COLUMNS));
            preview.setRows(buildDailyExternalRows(bundles));
            return preview;
        }
        if (SUMMARY_TYPE_WEEKLY.equals(summaryType)) {
            preview.setPreviewMode(PREVIEW_MODE_WEEKLY_FACTORY);
            preview.setParameterColumns(new ArrayList<>(WEEKLY_FACTORY_COLUMNS));
            preview.setRows(buildWeeklyFactoryRows(bundles));
            return preview;
        }
        if (SUMMARY_TYPE_HALF_MONTHLY.equals(summaryType)) {
            preview.setPreviewMode(PREVIEW_MODE_HALF_MONTHLY_TERMINAL);
            preview.setParameterColumns(new ArrayList<>(HALF_MONTHLY_TERMINAL_COLUMNS));
            preview.setRows(buildHalfMonthlyTerminalRows(bundles));
            return preview;
        }
        LinkedHashSet<String> parameterColumns = collectParameterColumns(bundles);
        preview.setParameterColumns(new ArrayList<>(parameterColumns));
        preview.setRows(buildPointRows(bundles, parameterColumns));
        return preview;
    }

    private List<SummaryReportPreviewRowVO> buildDailyInternalRows(List<SummaryTaskBundle> bundles) {
        Map<String, SummaryReportPreviewRowVO> rowMap = new LinkedHashMap<>();
        for (String timeBucket : INTERNAL_TIME_BUCKETS) {
            SummaryReportPreviewRowVO row = new SummaryReportPreviewRowVO();
            row.setRowKey(timeBucket);
            row.setTimeBucket(formatInternalTimeBucketLabel(timeBucket));
            row.setSamplingTimeLabel(formatInternalTimeBucketLabel(timeBucket));
            row.setPointName("");
            row.setSampleTypeLabel("");
            row.setSampleNo("");
            row.setSourceTaskCount(0);
            for (String column : DAILY_INTERNAL_COLUMNS) {
                row.getParameterValues().put(column, "");
            }
            rowMap.put(timeBucket, row);
        }
        for (SummaryTaskBundle bundle : bundles) {
            String timeBucket = resolveInternalTimeBucket(bundle.task.getSamplingTime());
            if (StrUtil.isBlank(timeBucket)) {
                continue;
            }
            SummaryReportPreviewRowVO row = rowMap.get(timeBucket);
            if (row == null) {
                continue;
            }
            row.setSourceTaskCount((row.getSourceTaskCount() == null ? 0 : row.getSourceTaskCount()) + 1);
            fillInternalRow(row, bundle);
        }
        return new ArrayList<>(rowMap.values());
    }

    private List<SummaryReportPreviewRowVO> buildDailyExternalRows(List<SummaryTaskBundle> bundles) {
        List<SummaryReportPreviewRowVO> rows = new ArrayList<>();
        Map<String, SummaryTaskBundle> latestBundleMap = new LinkedHashMap<>();
        for (SummaryTaskBundle bundle : bundles) {
            String sampleType = bundle == null || bundle.task == null ? null : bundle.task.getSampleType();
            if (!DAILY_EXTERNAL_SAMPLE_TYPES.contains(sampleType)) {
                continue;
            }
            SummaryTaskBundle existing = latestBundleMap.get(sampleType);
            if (existing == null || compareDailyExternalBundle(bundle, existing) > 0) {
                latestBundleMap.put(sampleType, bundle);
            }
        }
        for (String sampleType : DAILY_EXTERNAL_SAMPLE_TYPES) {
            SummaryTaskBundle bundle = latestBundleMap.get(sampleType);
            if (bundle == null) {
                continue;
            }
            SummaryReportPreviewRowVO row = new SummaryReportPreviewRowVO();
            row.setRowKey(StrUtil.blankToDefault(bundle.task.getTaskNo(), String.valueOf(bundle.task.getId())));
            row.setSamplingTime(bundle.task.getSamplingTime());
            row.setSamplingTimeLabel(formatDateTime(bundle.task.getSamplingTime()));
            row.setPointName(StrUtil.blankToDefault(bundle.task.getPointName(), "-"));
            row.setSampleTypeLabel(LabWorkflowConstants.getSampleTypeLabel(bundle.task.getSampleType()));
            row.setSampleNo(StrUtil.blankToDefault(bundle.sample == null ? bundle.task.getSampleNo() : bundle.sample.getSampleNo(), "-"));
            row.setSourceTaskCount(1);
            for (String column : DAILY_EXTERNAL_COLUMNS) {
                row.getParameterValues().put(column, "");
            }
            fillExternalRow(row, bundle);
            rows.add(row);
        }
        return rows;
    }

    private List<SummaryReportPreviewRowVO> buildWeeklyFactoryRows(List<SummaryTaskBundle> bundles) {
        Map<String, SummaryTaskBundle> latestFactoryMap = new LinkedHashMap<>();
        for (SummaryTaskBundle bundle : bundles) {
            if (bundle == null || bundle.task == null || !isFactoryBundle(bundle)) {
                continue;
            }
            String regionName = StrUtil.blankToDefault(bundle.regionName, DEFAULT_REGION_NAME);
            SummaryTaskBundle existing = latestFactoryMap.get(regionName);
            if (existing == null || compareDailyExternalBundle(bundle, existing) > 0) {
                latestFactoryMap.put(regionName, bundle);
            }
        }
        List<SummaryReportPreviewRowVO> rows = new ArrayList<>();
        for (Map.Entry<String, SummaryTaskBundle> entry : latestFactoryMap.entrySet()) {
            SummaryTaskBundle bundle = entry.getValue();
            SummaryReportPreviewRowVO row = new SummaryReportPreviewRowVO();
            row.setRowKey(entry.getKey());
            row.setPointName(entry.getKey());
            row.setSamplingTime(bundle.task.getSamplingTime());
            row.setSamplingTimeLabel(formatDateTime(bundle.task.getSamplingTime()));
            row.setSampleTypeLabel(LabWorkflowConstants.getSampleTypeLabel(bundle.task.getSampleType()));
            row.setSampleNo(StrUtil.blankToDefault(bundle.sample == null ? bundle.task.getSampleNo() : bundle.sample.getSampleNo(), "-"));
            row.setSourceTaskCount(1);
            for (String column : WEEKLY_FACTORY_COLUMNS) {
                row.getParameterValues().put(column, "");
            }
            fillWeeklyFactoryRow(row, bundle);
            rows.add(row);
        }
        rows.sort(Comparator.comparing(SummaryReportPreviewRowVO::getPointName, Comparator.nullsLast(String::compareTo)));
        return rows;
    }

    private List<SummaryReportPreviewRowVO> buildHalfMonthlyTerminalRows(List<SummaryTaskBundle> bundles) {
        Map<String, SummaryTaskBundle> latestTerminalMap = new LinkedHashMap<>();
        for (SummaryTaskBundle bundle : bundles) {
            if (bundle == null || bundle.task == null || !isTerminalBundle(bundle)) {
                continue;
            }
            String address = resolveTaskAddress(bundle);
            SummaryTaskBundle existing = latestTerminalMap.get(address);
            if (existing == null || compareApprovedResultBundle(bundle, existing) > 0) {
                latestTerminalMap.put(address, bundle);
            }
        }
        List<SummaryReportPreviewRowVO> rows = new ArrayList<>();
        for (Map.Entry<String, SummaryTaskBundle> entry : latestTerminalMap.entrySet()) {
            SummaryTaskBundle bundle = entry.getValue();
            SummaryReportPreviewRowVO row = new SummaryReportPreviewRowVO();
            row.setRowKey(entry.getKey());
            row.setPointName(entry.getKey());
            row.setSamplingTime(bundle.task.getSamplingTime());
            row.setSamplingTimeLabel(formatDateTime(bundle.task.getSamplingTime()));
            row.setSampleTypeLabel(LabWorkflowConstants.getSampleTypeLabel(bundle.task.getSampleType()));
            row.setSampleNo(StrUtil.blankToDefault(bundle.sample == null ? bundle.task.getSampleNo() : bundle.sample.getSampleNo(), "-"));
            row.setSourceTaskCount(1);
            for (String column : HALF_MONTHLY_TERMINAL_COLUMNS) {
                row.getParameterValues().put(column, "");
            }
            fillHalfMonthlyTerminalRow(row, bundle);
            rows.add(row);
        }
        rows.sort(Comparator.comparing(SummaryReportPreviewRowVO::getPointName, Comparator.nullsLast(String::compareTo)));
        return rows;
    }

    private List<SummaryReportPreviewRowVO> buildPointRows(List<SummaryTaskBundle> bundles, LinkedHashSet<String> parameterColumns) {
        Map<String, List<SummaryTaskBundle>> grouped = bundles.stream()
                .collect(Collectors.groupingBy(this::resolvePointGroupKey, LinkedHashMap::new, Collectors.toList()));
        List<SummaryReportPreviewRowVO> rows = new ArrayList<>();
        for (Map.Entry<String, List<SummaryTaskBundle>> entry : grouped.entrySet()) {
            List<SummaryTaskBundle> pointBundles = new ArrayList<>(entry.getValue());
            pointBundles.sort(Comparator
                    .comparing((SummaryTaskBundle item) -> item.task.getSamplingTime(), Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(item -> safeText(item.task.getTaskNo())));
            SummaryTaskBundle latest = pointBundles.get(0);
            SummaryReportPreviewRowVO row = new SummaryReportPreviewRowVO();
            row.setRowKey(entry.getKey());
            row.setPointName(StrUtil.blankToDefault(latest.task.getPointName(), "-"));
            row.setSamplingTime(latest.task.getSamplingTime());
            row.setSamplingTimeLabel(formatDateTime(latest.task.getSamplingTime()));
            row.setSampleTypeLabel(LabWorkflowConstants.getSampleTypeLabel(latest.task.getSampleType()));
            row.setSampleNo(StrUtil.blankToDefault(latest.sample == null ? latest.task.getSampleNo() : latest.sample.getSampleNo(), "-"));
            row.setSourceTaskCount(pointBundles.size());
            row.setParameterValues(buildParameterValueMap(parameterColumns, pointBundles));
            rows.add(row);
        }
        rows.sort(Comparator.comparing(SummaryReportPreviewRowVO::getPointName, Comparator.nullsLast(String::compareTo)));
        return rows;
    }

    private void fillInternalRow(SummaryReportPreviewRowVO row, SummaryTaskBundle bundle) {
        if (isSourceWaterBundle(bundle)) {
            fillInternalCell(row, "水源水-水温℃", bundle, "水温", "水温℃", "温度");
            fillInternalCell(row, "水源水-PH值", bundle, "PH值", "pH", "PH");
            fillInternalCell(row, "水源水-浑浊度NTU", bundle, "浑浊度", "浮浊度");
            fillInternalCell(row, "水源水-氨氮(mg/L)", bundle, "氨氮");
            return;
        }
        if (isPreFilterBundle(bundle)) {
            fillInternalCell(row, "滤前水-浑浊度NTU", bundle, "浑浊度", "浮浊度");
            return;
        }
        if (isFactoryBundle(bundle)) {
            fillInternalCell(row, "出厂水-菌落总数CFU/ml", bundle, "菌落总数");
            fillInternalCell(row, "出厂水-总大肠菌群MPN/100ml", bundle, "总大肠菌群");
            fillInternalCell(row, "出厂水-大肠埃希氏菌CFU/ml", bundle, "大肠埃希氏菌");
            fillInternalCell(row, "出厂水-浑浊度NTU", bundle, "浑浊度", "浮浊度");
            fillInternalCell(row, "出厂水-PH值", bundle, "PH值", "pH", "PH");
            fillInternalCell(row, "出厂水-色度(度)", bundle, "色度");
            fillInternalCell(row, "出厂水-臭和味", bundle, "臭和味");
            fillInternalCell(row, "出厂水-肉眼可见物", bundle, "肉眼可见物");
            fillInternalCell(row, "出厂水-余氯(mg/L)", bundle, "余氯", "游离余氯");
            fillInternalCell(row, "出厂水-高锰酸盐指数(mg/L)", bundle, "高锰酸盐指数");
        }
    }

    private void fillExternalRow(SummaryReportPreviewRowVO row, SummaryTaskBundle bundle) {
        fillExternalCell(row, "浑浊度(NTU)", bundle, "浑浊度", "浮浊度");
        fillExternalCell(row, "色度(度)", bundle, "色度");
        fillExternalCell(row, "臭和味", bundle, "臭和味");
        fillExternalCell(row, "肉眼可见物", bundle, "肉眼可见物");
        fillExternalCell(row, "游离余氯(mg/L)", bundle, "游离余氯", "余氯");
    }

    private void fillWeeklyFactoryRow(SummaryReportPreviewRowVO row, SummaryTaskBundle bundle) {
        fillWeeklyFactoryCell(row, "色度(度)", bundle, "色度");
        fillWeeklyFactoryCell(row, "浑浊度(NTU)", bundle, "浑浊度", "浮浊度");
        fillWeeklyFactoryCell(row, "臭和味", bundle, "臭和味");
        fillWeeklyFactoryCell(row, "肉眼可见物", bundle, "肉眼可见物");
        fillWeeklyFactoryCell(row, "余氯(mg/L)", bundle, "余氯", "游离余氯");
        fillWeeklyFactoryCell(row, "菌落总数(CFU/ml)", bundle, "菌落总数");
        fillWeeklyFactoryCell(row, "总大肠菌群(MPN/100ml)", bundle, "总大肠菌群");
        fillWeeklyFactoryCell(row, "大肠埃希氏菌(CFU/ml)", bundle, "大肠埃希氏菌");
        fillWeeklyFactoryCell(row, "高锰酸盐指数(mg/L)", bundle, "高锰酸盐指数");
    }

    private void fillHalfMonthlyTerminalRow(SummaryReportPreviewRowVO row, SummaryTaskBundle bundle) {
        fillWeeklyFactoryCell(row, "色度(度)", bundle, "色度");
        fillWeeklyFactoryCell(row, "浑浊度(NTU)", bundle, "浑浊度", "浮浊度");
        fillWeeklyFactoryCell(row, "PH", bundle, "PH值", "pH", "PH");
        fillWeeklyFactoryCell(row, "臭和味", bundle, "臭和味");
        fillWeeklyFactoryCell(row, "游离氯(mg/L)", bundle, "游离氯", "游离余氯", "余氯");
        fillWeeklyFactoryCell(row, "菌落总数(CFU/ml)", bundle, "菌落总数");
        fillWeeklyFactoryCell(row, "总大肠菌群(MPN/100ml)", bundle, "总大肠菌群");
        fillWeeklyFactoryCell(row, "高锰酸盐指数(mg/L)", bundle, "高锰酸盐指数");
    }

    private void fillInternalCell(SummaryReportPreviewRowVO row, String column, SummaryTaskBundle bundle, String... aliases) {
        String value = findApprovedValue(bundle, aliases);
        if (StrUtil.isBlank(value)) {
            return;
        }
        row.getParameterValues().put(column, value);
    }

    private void fillExternalCell(SummaryReportPreviewRowVO row, String column, SummaryTaskBundle bundle, String... aliases) {
        row.getParameterValues().put(column, findValue(bundle, aliases));
    }

    private void fillWeeklyFactoryCell(SummaryReportPreviewRowVO row, String column, SummaryTaskBundle bundle, String... aliases) {
        row.getParameterValues().put(column, findApprovedValue(bundle, aliases));
    }

    private String findValue(SummaryTaskBundle bundle, String... aliases) {
        return findValue(bundle == null ? null : bundle.parameterValueMap, aliases);
    }

    private String findApprovedValue(SummaryTaskBundle bundle, String... aliases) {
        return findValue(bundle == null ? null : bundle.approvedParameterValueMap, aliases);
    }

    private String findValue(Map<String, String> parameterValueMap, String... aliases) {
        if (parameterValueMap == null || parameterValueMap.isEmpty()) {
            return "";
        }
        for (String alias : aliases) {
            String match = findValueByAlias(parameterValueMap, alias);
            if (StrUtil.isNotBlank(match)) {
                return match;
            }
        }
        return "";
    }

    private String findValueByAlias(Map<String, String> values, String alias) {
        if (values == null || values.isEmpty() || StrUtil.isBlank(alias)) {
            return "";
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String parameterName = StrUtil.trim(entry.getKey());
            if (StrUtil.isBlank(parameterName)) {
                continue;
            }
            if (parameterName.equalsIgnoreCase(alias)
                    || parameterName.contains(alias)
                    || alias.contains(parameterName)) {
                return StrUtil.blankToDefault(StrUtil.trim(entry.getValue()), "");
            }
        }
        return "";
    }

    private boolean isSourceWaterBundle(SummaryTaskBundle bundle) {
        String sampleType = bundle == null || bundle.task == null ? null : bundle.task.getSampleType();
        String pointName = bundle == null || bundle.task == null ? null : bundle.task.getPointName();
        return LabWorkflowConstants.SampleType.SOURCE_WATER.equals(sampleType)
                || containsText(pointName, "水源");
    }

    private boolean isPreFilterBundle(SummaryTaskBundle bundle) {
        String sampleType = bundle == null || bundle.task == null ? null : bundle.task.getSampleType();
        String pointName = bundle == null || bundle.task == null ? null : bundle.task.getPointName();
        return LabWorkflowConstants.SampleType.RAW.equals(sampleType)
                || containsText(pointName, "滤前");
    }

    private boolean isFactoryBundle(SummaryTaskBundle bundle) {
        String sampleType = bundle == null || bundle.task == null ? null : bundle.task.getSampleType();
        String pointName = bundle == null || bundle.task == null ? null : bundle.task.getPointName();
        return LabWorkflowConstants.SampleType.FACTORY.equals(sampleType)
                || containsText(pointName, "出厂");
    }

    private boolean isTerminalBundle(SummaryTaskBundle bundle) {
        String sampleType = bundle == null || bundle.task == null ? null : bundle.task.getSampleType();
        String pointName = bundle == null || bundle.task == null ? null : bundle.task.getPointName();
        String pointType = bundle == null || bundle.point == null ? null : bundle.point.getPointType();
        return LabWorkflowConstants.SampleType.TERMINAL.equals(sampleType)
                || LabWorkflowConstants.PointType.TERMINAL.equals(pointType)
                || containsText(pointName, "末梢")
                || containsText(pointName, "管网");
    }

    private String resolveTaskAddress(SummaryTaskBundle bundle) {
        if (bundle == null || bundle.task == null) {
            return "-";
        }
        return StrUtil.blankToDefault(
                StrUtil.trim(bundle.task.getAddress()),
                StrUtil.blankToDefault(StrUtil.trim(bundle.task.getPointName()), "-"));
    }

    private String resolveInternalTimeBucket(LocalDateTime samplingTime) {
        if (samplingTime == null) {
            return "";
        }
        String bucket = TIME_FORMATTER.format(samplingTime);
        return INTERNAL_TIME_BUCKETS.contains(bucket) ? bucket : "";
    }

    private String formatInternalTimeBucketLabel(String timeBucket) {
        if (StrUtil.isBlank(timeBucket) || !timeBucket.startsWith("0")) {
            return timeBucket;
        }
        return timeBucket.substring(1);
    }

    private LinkedHashSet<String> collectParameterColumns(List<SummaryTaskBundle> bundles) {
        LinkedHashSet<String> columns = new LinkedHashSet<>();
        for (SummaryTaskBundle bundle : bundles) {
            for (String parameterName : bundle.expectedParameterNames) {
                if (StrUtil.isNotBlank(parameterName)) {
                    columns.add(parameterName);
                }
            }
            for (String parameterName : bundle.parameterValueMap.keySet()) {
                if (StrUtil.isNotBlank(parameterName)) {
                    columns.add(parameterName);
                }
            }
        }
        List<String> sorted = new ArrayList<>(columns);
        sorted.sort((left, right) -> {
            int leftIndex = resolvePreferredParameterIndex(left);
            int rightIndex = resolvePreferredParameterIndex(right);
            if (leftIndex != rightIndex) {
                return Integer.compare(leftIndex, rightIndex);
            }
            return left.compareTo(right);
        });
        return new LinkedHashSet<>(sorted);
    }

    private Map<String, String> buildParameterValueMap(Set<String> parameterColumns, List<SummaryTaskBundle> bundles) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String parameterName : parameterColumns) {
            values.put(parameterName, "-");
        }
        if (bundles == null || bundles.isEmpty()) {
            return values;
        }
        for (SummaryTaskBundle bundle : bundles) {
            for (Map.Entry<String, String> entry : bundle.parameterValueMap.entrySet()) {
                if (StrUtil.isBlank(entry.getKey()) || StrUtil.isBlank(entry.getValue())) {
                    continue;
                }
                if (!values.containsKey(entry.getKey()) || "-".equals(values.get(entry.getKey()))) {
                    values.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return values;
    }

    private void applyAccumulator(SummaryReportPreviewVO preview, SummaryAccumulator accumulator) {
        preview.setPointCount(accumulator.pointNames.size());
        preview.setTaskCount(accumulator.taskCount);
        preview.setSampleCount(accumulator.sampleCount);
        preview.setDetectedTaskCount(accumulator.detectedTaskCount);
        preview.setResultItemCount(accumulator.resultItemCount);
        preview.setLatestSamplingTime(accumulator.latestSamplingTime);
        preview.setLatestDetectionTime(accumulator.latestDetectionTime);
        preview.setReportStatus(accumulator.resolveStatus());
        preview.setReportStatusLabel(getReportStatusLabel(accumulator.resolveStatus()));
    }

    private SummaryPeriod resolveSummaryPeriod(String summaryType, LocalDateTime samplingTime) {
        if (samplingTime == null) {
            throw new BusinessException("采样任务缺少采样时间，无法生成汇总报表");
        }
        LocalDate date = samplingTime.toLocalDate();
        if (SUMMARY_TYPE_DAILY.equals(summaryType)) {
            return new SummaryPeriod(date, date);
        }
        if (SUMMARY_TYPE_WEEKLY.equals(summaryType)) {
            LocalDate start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate end = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            return new SummaryPeriod(start, end);
        }
        if (SUMMARY_TYPE_HALF_MONTHLY.equals(summaryType)) {
            YearMonth yearMonth = YearMonth.from(date);
            if (date.getDayOfMonth() <= 15) {
                return new SummaryPeriod(date.withDayOfMonth(1), date.withDayOfMonth(15));
            }
            return new SummaryPeriod(date.withDayOfMonth(16), date.withDayOfMonth(yearMonth.lengthOfMonth()));
        }
        throw new BusinessException("暂不支持的汇总报表类型");
    }

    private LocalDateTime resolveQueryDateFrom(String summaryType, LocalDate dateFrom) {
        LocalDate baseDate = dateFrom;
        if (baseDate == null) {
            LocalDate now = LocalDate.now();
            if (SUMMARY_TYPE_DAILY.equals(summaryType)) {
                baseDate = now.minusDays(30);
            } else {
                baseDate = now.minusDays(180);
            }
        }
        return baseDate.atStartOfDay();
    }

    private LocalDateTime resolveQueryDateTo(String summaryType, LocalDate dateTo) {
        LocalDate baseDate = dateTo == null ? LocalDate.now() : dateTo;
        return baseDate.atTime(LocalTime.MAX);
    }

    private void validateSummaryType(String summaryType) {
        String normalized = StrUtil.trim(summaryType);
        if (!SUMMARY_TYPE_DAILY.equals(normalized)
                && !SUMMARY_TYPE_WEEKLY.equals(normalized)
                && !SUMMARY_TYPE_HALF_MONTHLY.equals(normalized)) {
            throw new BusinessException("汇总报表类型不正确");
        }
    }

    private void validateSummaryAccess(String summaryType) {
        if (dataScopeHelper.isRole("STAFF") && !SUMMARY_TYPE_DAILY.equals(StrUtil.trim(summaryType))) {
            throw new BusinessException("员工角色只允许查看日报管理");
        }
    }

    private void validateDailyReportType(String summaryType, String dailyReportType, boolean required) {
        if (!SUMMARY_TYPE_DAILY.equals(summaryType)) {
            return;
        }
        String normalized = normalizeDailyReportType(dailyReportType);
        if (StrUtil.isBlank(normalized)) {
            if (required) {
                throw new BusinessException("日报类型不能为空");
            }
            return;
        }
        if (!DAILY_REPORT_TYPE_INTERNAL.equals(normalized) && !DAILY_REPORT_TYPE_EXTERNAL.equals(normalized)) {
            throw new BusinessException("日报类型不正确");
        }
    }

    private boolean isInternalRecordTemplate(String previewMode) {
        return PREVIEW_MODE_DAILY_INTERNAL.equals(previewMode);
    }

    private void applyTaskSamplerScope(LambdaQueryWrapper<SamplingTask> wrapper) {
        if (wrapper == null) {
            return;
        }
        if (dataScopeHelper.isAdmin()) {
            return;
        }
        Long currentUserId = dataScopeHelper.currentUserId();
        if (dataScopeHelper.isRole("STAFF") && currentUserId != null) {
            wrapper.and(item -> item
                    .eq(SamplingTask::getSamplerId, currentUserId)
                    .or()
                    .like(SamplingTask::getSamplerIds, wrapSamplerId(currentUserId)));
            return;
        }
        if (dataScopeHelper.onlySelfScope() && currentUserId != null) {
            wrapper.eq(SamplingTask::getCreatedBy, currentUserId);
        }
    }

    private boolean filterTaskByRegion(SamplingTask task, MonitoringPoint point, String regionName) {
        if (StrUtil.isBlank(regionName)) {
            return true;
        }
        return containsText(resolveRegionName(point), StrUtil.trim(regionName));
    }

    private boolean filterTaskByPoint(SamplingTask task, String pointName) {
        if (StrUtil.isBlank(pointName)) {
            return true;
        }
        return containsText(task.getPointName(), StrUtil.trim(pointName));
    }

    private boolean filterTaskBySummaryPlanCycle(SamplingTask task, SamplingPlan plan, String summaryType) {
        if (SUMMARY_TYPE_WEEKLY.equals(summaryType)) {
            return plan != null && LabWorkflowConstants.CycleType.DAILY.equals(plan.getCycleType());
        }
        if (SUMMARY_TYPE_HALF_MONTHLY.equals(summaryType)) {
            return plan != null && LabWorkflowConstants.CycleType.HALF_MONTHLY.equals(plan.getCycleType());
        }
        return true;
    }

    private boolean filterTaskByExpectedPeriod(SamplingTask task,
                                               String summaryType,
                                               LocalDate expectedStart,
                                               LocalDate expectedEnd) {
        if (StrUtil.isBlank(summaryType) || expectedStart == null || expectedEnd == null || task.getSamplingTime() == null) {
            return true;
        }
        SummaryPeriod period = resolveSummaryPeriod(summaryType, task.getSamplingTime());
        return expectedStart.equals(period.start) && expectedEnd.equals(period.end);
    }

    private boolean filterTaskByWeeklyFixedTime(SamplingTask task, String summaryType) {
        if (!SUMMARY_TYPE_WEEKLY.equals(summaryType)) {
            return true;
        }
        if (task == null || task.getSamplingTime() == null) {
            return false;
        }
        LocalDateTime samplingTime = task.getSamplingTime();
        return samplingTime.getDayOfWeek() == DayOfWeek.THURSDAY
                && samplingTime.getHour() == 10;
    }

    private String resolveRegionName(MonitoringPoint point) {
        return StrUtil.blankToDefault(point == null ? null : StrUtil.trim(point.getRegionName()), DEFAULT_REGION_NAME);
    }

    private List<String> parseExpectedParameters(String detectionItems) {
        if (StrUtil.isBlank(detectionItems)) {
            return Collections.emptyList();
        }
        return java.util.Arrays.stream(detectionItems.split(","))
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    private String formatResultValue(BigDecimal resultValue, String unit) {
        if (resultValue == null) {
            return null;
        }
        return resultValue.stripTrailingZeros().toPlainString();
    }

    private int compareSample(LabSample left, LabSample right) {
        return Comparator
                .comparing(LabSample::getSamplingTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(LabSample::getCreatedTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .compare(left, right);
    }

    private int compareRecord(DetectionRecord left, DetectionRecord right) {
        return Comparator
                .comparing(DetectionRecord::getDetectionTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(DetectionRecord::getCreatedTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .compare(left, right);
    }

    private int compareDailyExternalBundle(SummaryTaskBundle left, SummaryTaskBundle right) {
        return Comparator
                .comparing((SummaryTaskBundle item) -> item.task.getSamplingTime(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(item -> item.task.getCreatedTime(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(item -> safeText(item.task.getTaskNo()))
                .compare(left, right);
    }

    private int compareApprovedResultBundle(SummaryTaskBundle left, SummaryTaskBundle right) {
        int leftApproved = hasApprovedResult(left) ? 1 : 0;
        int rightApproved = hasApprovedResult(right) ? 1 : 0;
        if (leftApproved != rightApproved) {
            return Integer.compare(leftApproved, rightApproved);
        }
        return compareDailyExternalBundle(left, right);
    }

    private boolean hasApprovedResult(SummaryTaskBundle bundle) {
        return bundle != null
                && bundle.approvedParameterValueMap != null
                && bundle.approvedParameterValueMap.values().stream().anyMatch(StrUtil::isNotBlank);
    }

    private boolean containsText(String source, String keyword) {
        return StrUtil.containsIgnoreCase(StrUtil.blankToDefault(source, ""), StrUtil.blankToDefault(keyword, ""));
    }

    private List<SummaryReportListVO> slicePage(List<SummaryReportListVO> records, Long pageNum, Long pageSize) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        long safePageNum = pageNum == null || pageNum <= 0 ? 1L : pageNum;
        long safePageSize = pageSize == null || pageSize <= 0 ? 10L : pageSize;
        int fromIndex = (int) Math.min((safePageNum - 1) * safePageSize, records.size());
        int toIndex = (int) Math.min(fromIndex + safePageSize, records.size());
        return new ArrayList<>(records.subList(fromIndex, toIndex));
    }

    private String buildReportKey(String summaryType, String regionName, LocalDate start, LocalDate end) {
        return buildReportKey(summaryType, regionName, start, end, null);
    }

    private String buildReportKey(String summaryType,
                                  String regionName,
                                  LocalDate start,
                                  LocalDate end,
                                  String dailyReportType) {
        return summaryType + "|" + safeText(regionName) + "|" + DATE_FORMATTER.format(start) + "|" + DATE_FORMATTER.format(end)
                + "|" + StrUtil.blankToDefault(normalizeDailyReportType(dailyReportType), "-");
    }

    private String buildReportName(String summaryType, String regionName, String periodLabel) {
        return buildReportName(summaryType, regionName, periodLabel, null);
    }

    private String buildReportName(String summaryType, String regionName, String periodLabel, String dailyReportType) {
        if (SUMMARY_TYPE_DAILY.equals(summaryType) && StrUtil.isNotBlank(normalizeDailyReportType(dailyReportType))) {
            return regionName + periodLabel + getDailyReportTypeLabel(dailyReportType);
        }
        return regionName + periodLabel + getSummaryTypeLabel(summaryType);
    }

    private String buildPeriodLabel(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return "-";
        }
        if (start.equals(end)) {
            return DATE_FORMATTER.format(start);
        }
        return DATE_FORMATTER.format(start) + " ~ " + DATE_FORMATTER.format(end);
    }

    private String resolvePointGroupKey(SummaryTaskBundle bundle) {
        return bundle.task.getPointId() == null
                ? safeText(bundle.task.getPointName())
                : String.valueOf(bundle.task.getPointId());
    }

    private int resolvePreferredParameterIndex(String parameterName) {
        int index = PREFERRED_PARAMETER_ORDER.indexOf(parameterName);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String resolveWeekdayLabel(LocalDate date) {
        if (date == null) {
            return "";
        }
        switch (date.getDayOfWeek()) {
            case MONDAY:
                return "星期一";
            case TUESDAY:
                return "星期二";
            case WEDNESDAY:
                return "星期三";
            case THURSDAY:
                return "星期四";
            case FRIDAY:
                return "星期五";
            case SATURDAY:
                return "星期六";
            case SUNDAY:
                return "星期日";
            default:
                return "";
        }
    }

    private String resolveWeather(List<SummaryTaskBundle> bundles) {
        if (bundles == null) {
            return "";
        }
        return bundles.stream()
                .map(item -> item == null || item.task == null ? null : item.task.getWeather())
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse("");
    }

    private String resolveInspectorName(List<SummaryTaskBundle> bundles) {
        if (bundles == null) {
            return "";
        }
        return bundles.stream()
                .map(item -> item == null || item.record == null ? null : item.record.getDetectorName())
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining("、"));
    }

    private String resolveReporterName(List<SummaryTaskBundle> bundles) {
        String detectorNames = resolveInspectorName(bundles);
        if (StrUtil.isNotBlank(detectorNames)) {
            return detectorNames;
        }
        if (bundles == null) {
            return "";
        }
        return bundles.stream()
                .map(item -> item == null || item.task == null ? null : item.task.getSamplerName())
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining("、"));
    }

    private String getSummaryTypeLabel(String summaryType) {
        if (SUMMARY_TYPE_DAILY.equals(summaryType)) {
            return "日报";
        }
        if (SUMMARY_TYPE_WEEKLY.equals(summaryType)) {
            return "周报";
        }
        if (SUMMARY_TYPE_HALF_MONTHLY.equals(summaryType)) {
            return "半月报";
        }
        return StrUtil.blankToDefault(summaryType, "汇总报表");
    }

    private String normalizeDailyReportType(String dailyReportType) {
        return StrUtil.trimToEmpty(dailyReportType).toUpperCase();
    }

    private String getDailyReportTypeLabel(String dailyReportType) {
        if (DAILY_REPORT_TYPE_INTERNAL.equals(normalizeDailyReportType(dailyReportType))) {
            return "对内日报";
        }
        if (DAILY_REPORT_TYPE_EXTERNAL.equals(normalizeDailyReportType(dailyReportType))) {
            return "对外日报";
        }
        return "";
    }

    private String getReportStatusLabel(String reportStatus) {
        if (REPORT_STATUS_COMPLETE.equals(reportStatus)) {
            return "已完成";
        }
        if (REPORT_STATUS_PARTIAL.equals(reportStatus)) {
            return "部分完成";
        }
        if (REPORT_STATUS_PENDING.equals(reportStatus)) {
            return "待补全";
        }
        return StrUtil.blankToDefault(reportStatus, "-");
    }

    private String wrapSamplerId(Long samplerId) {
        return samplerId == null ? null : "," + samplerId + ",";
    }

    private String safeText(String value) {
        return StrUtil.blankToDefault(StrUtil.trim(value), "");
    }

    private String sanitizeFileName(String rawName) {
        return StrUtil.blankToDefault(rawName, "汇总报表")
                .replace("\\", "_")
                .replace("/", "_")
                .replace(":", "_")
                .replace("*", "_")
                .replace("?", "_")
                .replace("\"", "_")
                .replace("<", "_")
                .replace(">", "_")
                .replace("|", "_");
    }

    private static class SummaryTaskBundle {

        private SamplingTask task;

        private SamplingPlan plan;

        private MonitoringPoint point;

        private LabSample sample;

        private DetectionRecord record;

        private List<DetectionItem> items = Collections.emptyList();

        private String regionName;

        private List<String> expectedParameterNames = Collections.emptyList();

        private Map<String, String> parameterValueMap = new LinkedHashMap<>();

        private Map<String, String> approvedParameterValueMap = new LinkedHashMap<>();

        private int filledResultCount;
    }

    private static class SummaryPeriod {

        private final LocalDate start;

        private final LocalDate end;

        private SummaryPeriod(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
    }

    private class SummaryAccumulator {

        private final String summaryType;

        private final String regionName;

        private final SummaryPeriod period;

        private final Set<String> pointNames = new LinkedHashSet<>();

        private int taskCount;

        private int sampleCount;

        private int detectedTaskCount;

        private int resultItemCount;

        private LocalDateTime latestSamplingTime;

        private LocalDateTime latestDetectionTime;

        private SummaryAccumulator(String summaryType, String regionName, SummaryPeriod period) {
            this.summaryType = summaryType;
            this.regionName = regionName;
            this.period = period;
        }

        private void add(SummaryTaskBundle bundle) {
            if (bundle == null || bundle.task == null) {
                return;
            }
            this.taskCount++;
            this.pointNames.add(StrUtil.blankToDefault(bundle.task.getPointName(), "-"));
            if (bundle.sample != null) {
                this.sampleCount++;
            }
            if (!bundle.parameterValueMap.isEmpty()) {
                this.detectedTaskCount++;
            }
            this.resultItemCount += Math.max(bundle.filledResultCount, 0);
            if (bundle.task.getSamplingTime() != null
                    && (this.latestSamplingTime == null || bundle.task.getSamplingTime().isAfter(this.latestSamplingTime))) {
                this.latestSamplingTime = bundle.task.getSamplingTime();
            }
            LocalDateTime detectionTime = bundle.record == null ? null : bundle.record.getDetectionTime();
            if (detectionTime != null
                    && (this.latestDetectionTime == null || detectionTime.isAfter(this.latestDetectionTime))) {
                this.latestDetectionTime = detectionTime;
            }
        }

        private String resolveStatus() {
            if (this.detectedTaskCount <= 0) {
                return REPORT_STATUS_PENDING;
            }
            if (this.detectedTaskCount < this.taskCount) {
                return REPORT_STATUS_PARTIAL;
            }
            return REPORT_STATUS_COMPLETE;
        }

        private SummaryReportListVO toListVO(String dailyReportType) {
            SummaryReportListVO vo = new SummaryReportListVO();
            vo.setReportKey(buildReportKey(this.summaryType, this.regionName, this.period.start, this.period.end, dailyReportType));
            vo.setSummaryType(this.summaryType);
            vo.setSummaryTypeLabel(getSummaryTypeLabel(this.summaryType));
            vo.setDailyReportType(normalizeDailyReportType(dailyReportType));
            vo.setDailyReportTypeLabel(getDailyReportTypeLabel(dailyReportType));
            vo.setRegionName(this.regionName);
            vo.setPeriodStart(this.period.start);
            vo.setPeriodEnd(this.period.end);
            vo.setPeriodLabel(buildPeriodLabel(this.period.start, this.period.end));
            vo.setReportName(buildReportName(this.summaryType, this.regionName, vo.getPeriodLabel(), dailyReportType));
            vo.setPointCount(this.pointNames.size());
            vo.setTaskCount(this.taskCount);
            vo.setSampleCount(this.sampleCount);
            vo.setDetectedTaskCount(this.detectedTaskCount);
            vo.setResultItemCount(this.resultItemCount);
            vo.setReportStatus(resolveStatus());
            vo.setReportStatusLabel(getReportStatusLabel(resolveStatus()));
            vo.setLatestSamplingTime(this.latestSamplingTime);
            vo.setLatestDetectionTime(this.latestDetectionTime);
            return vo;
        }
    }
}
