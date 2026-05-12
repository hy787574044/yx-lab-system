package com.yx.lab.modules.report.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.detection.entity.DetectionItem;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionItemMapper;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.report.dto.ReportQuery;
import com.yx.lab.modules.report.dto.ReportTemplateSaveCommand;
import com.yx.lab.modules.report.entity.LabReport;
import com.yx.lab.modules.report.entity.ReportPushRecord;
import com.yx.lab.modules.report.entity.ReportTemplate;
import com.yx.lab.modules.report.mapper.LabReportMapper;
import com.yx.lab.modules.report.mapper.ReportPushRecordMapper;
import com.yx.lab.modules.report.mapper.ReportTemplateMapper;
import com.yx.lab.modules.report.vo.ReportPreviewItemVO;
import com.yx.lab.modules.report.vo.ReportPreviewVO;
import com.yx.lab.modules.review.entity.ReviewRecord;
import com.yx.lab.modules.review.mapper.ReviewRecordMapper;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.sample.service.LabSampleService;
import com.yx.lab.modules.storage.service.StorageService;
import com.yx.lab.modules.system.entity.LabUser;
import com.yx.lab.modules.system.mapper.LabUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final LabReportMapper labReportMapper;

    private final ReportTemplateMapper reportTemplateMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    private final DetectionItemMapper detectionItemMapper;

    private final LabSampleService labSampleService;

    private final LabSampleMapper labSampleMapper;

    private final ReviewRecordMapper reviewRecordMapper;

    private final LabUserMapper labUserMapper;

    private final ReportPushRecordMapper reportPushRecordMapper;

    private final StorageService storageService;

    /**
     * 分页查询报告列表。
     *
     * @param query 查询条件
     * @return 报告分页结果
     */
    public PageResult<LabReport> page(ReportQuery query) {
        Page<LabReport> page = labReportMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<LabReport>()
                        .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                                .like(LabReport::getReportName, query.getKeyword())
                                .or()
                                .like(LabReport::getSealNo, query.getKeyword())
                                .or()
                                .like(LabReport::getSampleNo, query.getKeyword()))
                        .eq(StrUtil.isNotBlank(query.getReportType()), LabReport::getReportType, query.getReportType())
                        .eq(StrUtil.isNotBlank(query.getReportStatus()), LabReport::getReportStatus, query.getReportStatus())
                        .orderByDesc(LabReport::getGeneratedTime));
        page.getRecords().forEach(this::refreshStoredContentSnapshotIfNeeded);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    /**
     * 分页查询报告模板列表。
     *
     * @param query 分页参数
     * @return 模板分页结果
     */
    public PageResult<ReportTemplate> templatePage(com.yx.lab.common.model.PageQuery query) {
        Page<ReportTemplate> page = reportTemplateMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<ReportTemplate>().orderByDesc(ReportTemplate::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    /**
     * 新增报告模板。
     *
     * @param command 模板保存参数
     */
    public void saveTemplate(ReportTemplateSaveCommand command) {
        ReportTemplate template = new ReportTemplate();
        applyTemplateCommand(template, command);
        reportTemplateMapper.insert(template);
    }

    /**
     * 更新报告模板。
     *
     * @param id 模板ID
     * @param command 模板保存参数
     */
    public void updateTemplate(Long id, ReportTemplateSaveCommand command) {
        ReportTemplate template = requireTemplate(id);
        applyTemplateCommand(template, command);
        reportTemplateMapper.updateById(template);
    }

    /**
     * 删除报告模板。
     *
     * @param id 模板ID
     */
    public void deleteTemplate(Long id) {
        reportTemplateMapper.deleteById(requireTemplate(id).getId());
    }

    /**
     * 正式发布报告，并生成报告产物及推送记录。
     *
     * @param id 报告ID
     */
    public void publish(Long id) {
        LabReport existing = requireReport(id);
        if (!LabWorkflowConstants.canPublishReport(existing.getReportStatus())) {
            throw new BusinessException("当前报告状态不允许发布");
        }
        // 发布前必须保证正式产物已经可预览、可打印，避免只改状态未落文件。
        ensureReportArtifact(existing);

        CurrentUser currentUser = requireCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        LabReport report = new LabReport();
        report.setId(existing.getId());
        report.setReportStatus(LabWorkflowConstants.ReportStatus.PUBLISHED);
        report.setPublishedTime(now);
        report.setPublishedBy(currentUser.getUserId());
        report.setPublishedByName(currentUser.getRealName());

        // 发布成功后同步记录推送状态，便于后续追踪是否已下发到外部平台或接收人。
        PushResult pushResult = pushReport(existing, now);
        report.setPushStatus(pushResult.pushStatus);
        report.setLastPushTime(now);
        report.setLastPushMessage(pushResult.pushMessage);
        labReportMapper.updateById(report);

        if (existing.getSampleId() != null) {
            labSampleService.appendTrace(existing.getSampleId(),
                    "报告已发布：报告名称=" + existing.getReportName()
                            + "，发布人=" + currentUser.getRealName()
                            + "，推送结果=" + pushResult.pushMessage);
        }
    }

    /**
     * 取消发布报告。
     *
     * @param id 报告ID
     */
    public void unpublish(Long id) {
        LabReport existing = requireReport(id);
        if (!LabWorkflowConstants.canUnpublishReport(existing.getReportStatus())) {
            throw new BusinessException("当前报告状态不允许取消发布");
        }
        LabReport report = new LabReport();
        report.setId(existing.getId());
        report.setReportStatus(LabWorkflowConstants.ReportStatus.DRAFT);
        report.setPushStatus("CANCELLED");
        report.setLastPushTime(LocalDateTime.now());
        report.setLastPushMessage("报告已取消发布");
        labReportMapper.updateById(report);
        if (existing.getSampleId() != null) {
            labSampleService.appendTrace(existing.getSampleId(),
                    "报告已取消发布：报告名称=" + existing.getReportName());
        }
    }

    /**
     * 在检测流程审核通过后生成正式报告。
     *
     * @param sample 样品信息
     * @param record 检测主流程
     */
    public void createApprovedReport(LabSample sample, DetectionRecord record) {
        ReportTemplate template = reportTemplateMapper.selectOne(new LambdaQueryWrapper<ReportTemplate>()
                .eq(ReportTemplate::getDefaultTemplate, 1)
                .last("limit 1"));

        String content;
        // 没有默认模板时仍可生成基础报告文本，保证审核通过后一定有正式产物。
        if (template == null) {
            content = "样品编号：" + sample.getSampleNo()
                    + "\n封签编号：" + sample.getSealNo()
                    + "\n监测点位：" + sample.getPointName()
                    + "\n检测结果：" + LabWorkflowConstants.getDetectionResultLabel(record.getDetectionResult());
        } else {
            content = template.getTemplateContent()
                    .replace("${sampleNo}", sample.getSampleNo())
                    .replace("${sealNo}", StrUtil.blankToDefault(sample.getSealNo(), "-"))
                    .replace("${pointName}", sample.getPointName())
                    .replace("${detectionType}", "")
                    .replace("${detectionResult}", LabWorkflowConstants.getDetectionResultLabel(record.getDetectionResult()));
        }

        LabReport report = new LabReport();
        report.setReportName(sample.getSampleNo() + "-检测报告");
        report.setReportType(LabWorkflowConstants.ReportType.DAILY);
        report.setGeneratedTime(LocalDateTime.now());
        report.setSampleId(sample.getId());
        report.setSampleNo(sample.getSampleNo());
        report.setSealNo(sample.getSealNo());
        report.setDetectionRecordId(record.getId());
        report.setReportStatus(LabWorkflowConstants.ReportStatus.GENERATED);
        report.setContentSnapshot(content);
        report.setPushStatus("PENDING");
        // 报告入库时同步生成 HTML 产物，后续预览和打印直接复用该正式文件。
        report.setFilePath(writeReportArtifact(report, sample, record, loadDetectionItems(record.getId()), loadLatestReview(sample.getId())));
        labReportMapper.insert(report);
        labSampleService.appendTrace(sample.getId(),
                "已生成报告：封签号=" + sample.getSealNo()
                        + "，报告名称=" + report.getReportName()
                        + "，状态=已生成");
    }

    /**
     * 预览报告打印内容。
     *
     * @param id 报告ID
     * @return 报告预览字节流
     */
    public byte[] preview(Long id) {
        LabReport report = requireReport(id);
        refreshStoredPresentation(report, true);
        LabSample sample = report.getSampleId() == null ? null : labSampleMapper.selectById(report.getSampleId());
        DetectionRecord detectionRecord = report.getDetectionRecordId() == null
                ? null
                : detectionRecordMapper.selectById(report.getDetectionRecordId());
        List<DetectionItem> detectionItems = loadDetectionItems(report.getDetectionRecordId());
        ReviewRecord latestReview = loadLatestReview(report.getSampleId());
        String html = buildDetailedReportHtml(report, sample, detectionRecord, detectionItems, latestReview);
        return html.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 查询报告预览所需的结构化数据。
     *
     * @param id 报告ID
     * @return 报告预览数据
     */
    public ReportPreviewVO previewData(Long id) {
        LabReport report = requireReport(id);
        // 预览数据以最新持久化产物为准，避免页面看到的是过期内容。
        refreshStoredContentSnapshotIfNeeded(report);
        LabSample sample = report.getSampleId() == null ? null : labSampleMapper.selectById(report.getSampleId());
        DetectionRecord detectionRecord = report.getDetectionRecordId() == null
                ? null
                : detectionRecordMapper.selectById(report.getDetectionRecordId());
        List<DetectionItem> detectionItems = loadDetectionItems(report.getDetectionRecordId());
        ReviewRecord latestReview = loadLatestReview(report.getSampleId());
        return buildPreviewVO(report, sample, detectionRecord, detectionItems, latestReview);
    }

    private ReportTemplate requireTemplate(Long id) {
        ReportTemplate template = reportTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("报告模板不存在");
        }
        return template;
    }

    private void applyTemplateCommand(ReportTemplate template, ReportTemplateSaveCommand command) {
        template.setReportType(StrUtil.trim(command.getReportType()));
        template.setTemplateName(StrUtil.trim(command.getTemplateName()));
        template.setDefaultTemplate(command.getDefaultTemplate());
        template.setTemplateContent(StrUtil.trim(command.getTemplateContent()));
        template.setRemark(StrUtil.trim(command.getRemark()));
    }

    private LabReport requireReport(Long id) {
        LabReport report = labReportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException("报告不存在");
        }
        return report;
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("当前登录用户信息失效，请重新登录");
        }
        return currentUser;
    }

    private void ensureReportArtifact(LabReport report) {
        if (report == null) {
            return;
        }
        refreshStoredPresentation(report, true);
    }

    private String writeReportArtifact(LabReport report,
                                       LabSample sample,
                                       DetectionRecord detectionRecord,
                                       List<DetectionItem> detectionItems,
                                       ReviewRecord latestReview) {
        String safeSampleNo = sample == null ? "report" : StrUtil.blankToDefault(sample.getSampleNo(), "report");
        String fileName = "reports/" + safeSampleNo + "-" + System.currentTimeMillis() + ".html";
        try {
            return storageService.storeText(fileName, buildDetailedReportHtml(report, sample, detectionRecord, detectionItems, latestReview));
        } catch (IOException ex) {
            throw new BusinessException("报告文件写入失败：" + ex.getMessage());
        }
    }

    private String buildDetailedReportHtml(LabReport report,
                                           LabSample sample,
                                           DetectionRecord detectionRecord,
                                           List<DetectionItem> detectionItems,
                                           ReviewRecord latestReview) {
        String reportName = escapeHtml(StrUtil.blankToDefault(
                report == null ? null : report.getReportName(),
                (sample == null ? "检测报告" : sample.getSampleNo() + "-检测报告")));
        String sampleNo = sample == null ? "-" : StrUtil.blankToDefault(sample.getSampleNo(), "-");
        String sealNo = sample == null ? "-" : StrUtil.blankToDefault(sample.getSealNo(), "-");
        String pointName = sample == null ? "-" : StrUtil.blankToDefault(sample.getPointName(), "-");
        String sampleType = sample == null ? "-" : StrUtil.blankToDefault(LabWorkflowConstants.getSampleTypeLabel(sample.getSampleType()), "-");
        String samplingTime = formatDateTime(sample == null ? null : sample.getSamplingTime());
        String sealTime = formatDateTime(sample == null ? null : sample.getSealTime());
        String samplerName = sample == null ? "-" : StrUtil.blankToDefault(sample.getSamplerName(), "-");
        String weather = sample == null ? "-" : StrUtil.blankToDefault(sample.getWeather(), "-");
        String storageCondition = sample == null ? "-" : StrUtil.blankToDefault(sample.getStorageCondition(), "-");
        String sampleRemark = sample == null ? "-" : StrUtil.blankToDefault(sample.getRemark(), "-");
        String sampleStatus = sample == null ? "-" : StrUtil.blankToDefault(LabWorkflowConstants.getSampleStatusLabel(sample.getSampleStatus()), "-");
        String traceLog = sample == null ? "" : translateWorkflowText(StrUtil.blankToDefault(sample.getTraceLog(), ""));
        String resultSummary = sample == null ? "-" : translateWorkflowText(StrUtil.blankToDefault(sample.getResultSummary(), "-"));
        String detectionResult = detectionRecord == null ? "-" : StrUtil.blankToDefault(LabWorkflowConstants.getDetectionResultLabel(detectionRecord.getDetectionResult()), "-");
        String detectorName = detectionRecord == null ? "-" : StrUtil.blankToDefault(detectionRecord.getDetectorName(), "-");
        String detectionTime = formatDateTime(detectionRecord == null ? null : detectionRecord.getDetectionTime());
        String recordRemark = detectionRecord == null ? "-" : translateWorkflowText(StrUtil.blankToDefault(detectionRecord.getAbnormalRemark(), "-"));
        String reviewTime = formatDateTime(latestReview == null ? null : latestReview.getReviewTime());
        String reviewerName = latestReview == null ? "-" : StrUtil.blankToDefault(latestReview.getReviewerName(), "-");
        String reviewResult = latestReview == null ? "-" : StrUtil.blankToDefault(LabWorkflowConstants.getReviewResultLabel(latestReview.getReviewResult()), "-");
        String reviewRemark = latestReview == null ? "-" : translateWorkflowText(StrUtil.blankToDefault(latestReview.getReviewRemark(), "-"));
        String rejectReason = latestReview == null ? "-" : translateWorkflowText(StrUtil.blankToDefault(latestReview.getRejectReason(), "-"));

        List<DetectionItem> items = detectionItems == null ? Collections.emptyList() : detectionItems;
        int abnormalCount = (int) items.stream().filter(item -> Integer.valueOf(1).equals(item.getExceedFlag())).count();
        int normalCount = Math.max(0, items.size() - abnormalCount);

        StringBuilder html = new StringBuilder(16384);
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>")
                .append(reportName)
                .append("</title>")
                .append("<style>")
                .append("body{margin:0;background:#eef3fb;font-family:SimSun,Microsoft YaHei,sans-serif;color:#1f2937;}")
                .append(".page{max-width:1280px;margin:0 auto;padding:28px 30px 36px;}")
                .append(".report-card{background:#fff;border-radius:18px;box-shadow:0 16px 40px rgba(15,23,42,.08);padding:28px 32px;}")
                .append(".report-title{font-size:30px;font-weight:700;letter-spacing:.5px;margin:0 0 10px;}")
                .append(".report-subtitle{font-size:14px;color:#5b6b85;margin:0 0 24px;}")
                .append(".summary-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px;margin-bottom:24px;}")
                .append(".summary-item{background:linear-gradient(180deg,#f7faff,#eef4ff);border:1px solid #dce7fb;border-radius:14px;padding:14px 16px;}")
                .append(".summary-item span{display:block;font-size:12px;color:#6b7a90;margin-bottom:8px;}")
                .append(".summary-item strong{font-size:22px;color:#1d4ed8;}")
                .append(".section{margin-top:26px;}")
                .append(".section-title{font-size:18px;font-weight:700;margin:0 0 14px;padding-left:12px;border-left:4px solid #3b82f6;}")
                .append(".info-table,.result-table{width:100%;border-collapse:collapse;table-layout:fixed;}")
                .append(".info-table td,.result-table th,.result-table td{border:1px solid #d9e2f1;padding:10px 12px;vertical-align:top;word-break:break-word;}")
                .append(".info-table td.label{width:13%;background:#f7faff;color:#50617c;font-weight:700;}")
                .append(".result-table th{background:#eef4ff;color:#334155;font-weight:700;}")
                .append(".tag{display:inline-block;padding:4px 12px;border-radius:999px;font-size:12px;font-weight:700;}")
                .append(".tag-success{background:#e8f8ef;color:#047857;}")
                .append(".tag-danger{background:#fdecec;color:#dc2626;}")
                .append(".tag-warning{background:#fff4dd;color:#d97706;}")
                .append(".tag-info{background:#eaf2ff;color:#2563eb;}")
                .append(".compare-ok{color:#047857;font-weight:700;}")
                .append(".compare-bad{color:#dc2626;font-weight:700;}")
                .append(".trace-box{white-space:pre-wrap;line-height:1.85;background:#f8fbff;border:1px solid #dce7fb;border-radius:14px;padding:16px 18px;}")
                .append(".footer-note{margin-top:28px;font-size:12px;color:#64748b;line-height:1.9;}")
                .append("@media print{body{background:#fff;}.page{padding:0;}.report-card{box-shadow:none;border-radius:0;padding:0;}}")
                .append("</style></head><body><div class=\"page\"><div class=\"report-card\">");

        html.append("<h1 class=\"report-title\">").append(reportName).append("</h1>")
                .append("<p class=\"report-subtitle\">本报告根据样品登记、检测结果、审核结论自动汇总生成，可用于预览、打印和归档留痕。</p>");

        html.append("<div class=\"summary-grid\">")
                .append(buildSummaryItem("样品编号", sampleNo))
                .append(buildSummaryItem("封签编号", sealNo))
                .append(buildSummaryItem("检测项数", String.valueOf(items.size())))
                .append(buildSummaryItem("综合结果", detectionResult))
                .append(buildSummaryItem("正常项数", String.valueOf(normalCount)))
                .append(buildSummaryItem("异常项数", String.valueOf(abnormalCount)))
                .append(buildSummaryItem("检测人员", detectorName))
                .append(buildSummaryItem("审核结论", reviewResult))
                .append("</div>");

        html.append("<div class=\"section\"><h2 class=\"section-title\">样品基础信息</h2>")
                .append("<table class=\"info-table\">")
                .append(infoRow("样品编号", sampleNo, "封签编号", sealNo))
                .append(infoRow("监测点位", pointName, "样品类型", sampleType))
                .append(infoRow("采样时间", samplingTime, "封签时间", sealTime))
                .append(infoRow("采样人员", samplerName, "天气情况", weather))
                .append(infoRow("存储条件", storageCondition, "样品状态", sampleStatus))
                .append(infoRow("样品备注", sampleRemark, "结果摘要", resultSummary))
                .append("</table></div>");

        html.append("<div class=\"section\"><h2 class=\"section-title\">检测与审核信息</h2>")
                .append("<table class=\"info-table\">")
                .append(infoRow("检测时间", detectionTime, "检测人员", detectorName))
                .append(infoRow("综合结果", detectionResult, "流程备注", recordRemark))
                .append(infoRow("审核时间", reviewTime, "审核人员", reviewerName))
                .append(infoRow("审核结论", reviewResult, "审核意见", reviewRemark))
                .append(infoRow("驳回原因", rejectReason, "报告生成时间", formatDateTime(report == null ? null : report.getGeneratedTime())))
                .append("</table></div>");

        html.append("<div class=\"section\"><h2 class=\"section-title\">化验结果明细</h2>")
                .append("<table class=\"result-table\">")
                .append("<thead><tr>")
                .append("<th style=\"width:60px;\">序号</th>")
                .append("<th style=\"width:120px;\">检测参数</th>")
                .append("<th style=\"width:150px;\">检测方法</th>")
                .append("<th style=\"width:90px;\">单位</th>")
                .append("<th style=\"width:120px;\">标准范围</th>")
                .append("<th style=\"width:160px;\">参考标准</th>")
                .append("<th style=\"width:90px;\">检测值</th>")
                .append("<th style=\"width:140px;\">结果对比</th>")
                .append("<th style=\"width:110px;\">单项判定</th>")
                .append("<th style=\"width:110px;\">流程状态</th>")
                .append("</tr></thead><tbody>");

        if (items.isEmpty()) {
            html.append("<tr><td colspan=\"10\" style=\"text-align:center;color:#64748b;\">当前报告暂无化验结果明细数据</td></tr>");
        } else {
            int index = 1;
            for (DetectionItem item : items) {
                String compareText = buildCompareText(item);
                boolean exceeded = Integer.valueOf(1).equals(item.getExceedFlag());
                html.append("<tr>")
                        .append("<td style=\"text-align:center;\">").append(index++).append("</td>")
                        .append("<td>").append(escapeHtml(StrUtil.blankToDefault(item.getParameterName(), "-"))).append("</td>")
                        .append("<td>").append(escapeHtml(StrUtil.blankToDefault(item.getMethodName(), "-"))).append("</td>")
                        .append("<td>").append(escapeHtml(StrUtil.blankToDefault(item.getUnit(), "-"))).append("</td>")
                        .append("<td>").append(escapeHtml(formatStandardRange(item.getStandardMin(), item.getStandardMax(), item.getUnit()))).append("</td>")
                        .append("<td>").append(escapeHtml(StrUtil.blankToDefault(item.getReferenceStandard(), "-"))).append("</td>")
                        .append("<td>").append(escapeHtml(formatDecimal(item.getResultValue()))).append("</td>")
                        .append("<td class=\"").append(exceeded ? "compare-bad" : "compare-ok").append("\">")
                        .append(escapeHtml(compareText))
                        .append("</td>")
                        .append("<td>")
                        .append(renderTag(exceeded ? "异常" : "正常", exceeded ? "danger" : "success"))
                        .append("</td>")
                        .append("<td>")
                        .append(renderTag(getItemStatusLabel(item.getItemStatus()), mapItemStatusTag(item.getItemStatus())))
                        .append("</td>")
                        .append("</tr>");
            }
        }
        html.append("</tbody></table></div>");

        html.append("<div class=\"section\"><h2 class=\"section-title\">流程轨迹与留痕</h2>")
                .append("<div class=\"trace-box\">")
                .append(escapeHtml(StrUtil.isBlank(traceLog) ? "当前样品暂无流程轨迹记录" : traceLog))
                .append("</div></div>");

        html.append("<div class=\"footer-note\">")
                .append("本报告内容由样品登记、检测结果、审核结论自动汇总生成，适用于页面预览、A4打印和档案留存。")
                .append("若后续检测数据、审核结论或样品信息发生变更，请重新生成或刷新报告产物。")
                .append("</div>");

        html.append("</div></div></body></html>");
        return html.toString();
    }

    private ReportPreviewVO buildPreviewVO(LabReport report,
                                           LabSample sample,
                                           DetectionRecord detectionRecord,
                                           List<DetectionItem> detectionItems,
                                           ReviewRecord latestReview) {
        ReportPreviewVO vo = new ReportPreviewVO();
        vo.setReportId(report == null ? null : report.getId());
        vo.setReportName(StrUtil.blankToDefault(report == null ? null : report.getReportName(), "检测报告"));
        vo.setReportTypeLabel(resolveReportTypeLabel(report == null ? null : report.getReportType()));
        vo.setReportStatusLabel(LabWorkflowConstants.getReportStatusLabel(report == null ? null : report.getReportStatus()));
        vo.setGeneratedTime(formatDateTime(report == null ? null : report.getGeneratedTime()));
        vo.setPublishedTime(formatDateTime(report == null ? null : report.getPublishedTime()));
        vo.setPublishedByName(StrUtil.blankToDefault(report == null ? null : report.getPublishedByName(), "-"));
        vo.setPushStatusLabel(LabWorkflowConstants.getPushStatusLabel(report == null ? null : report.getPushStatus()));
        vo.setLastPushTime(formatDateTime(report == null ? null : report.getLastPushTime()));
        vo.setLastPushMessage(translateWorkflowText(StrUtil.blankToDefault(report == null ? null : report.getLastPushMessage(), "-")));

        vo.setSampleNo(StrUtil.blankToDefault(sample == null ? null : sample.getSampleNo(), "-"));
        vo.setSealNo(StrUtil.blankToDefault(sample == null ? null : sample.getSealNo(), "-"));
        vo.setPointName(StrUtil.blankToDefault(sample == null ? null : sample.getPointName(), "-"));
        vo.setSampleTypeLabel(StrUtil.blankToDefault(LabWorkflowConstants.getSampleTypeLabel(sample == null ? null : sample.getSampleType()), "-"));
        vo.setSamplingTime(formatDateTime(sample == null ? null : sample.getSamplingTime()));
        vo.setSealTime(formatDateTime(sample == null ? null : sample.getSealTime()));
        vo.setSamplerName(StrUtil.blankToDefault(sample == null ? null : sample.getSamplerName(), "-"));
        vo.setWeather(StrUtil.blankToDefault(sample == null ? null : sample.getWeather(), "-"));
        vo.setStorageCondition(StrUtil.blankToDefault(sample == null ? null : sample.getStorageCondition(), "-"));
        vo.setSampleStatusLabel(StrUtil.blankToDefault(LabWorkflowConstants.getSampleStatusLabel(sample == null ? null : sample.getSampleStatus()), "-"));
        vo.setResultSummary(translateWorkflowText(StrUtil.blankToDefault(sample == null ? null : sample.getResultSummary(), "-")));
        vo.setSampleRemark(StrUtil.blankToDefault(sample == null ? null : sample.getRemark(), "-"));
        vo.setTraceLog(translateWorkflowText(StrUtil.blankToDefault(sample == null ? null : sample.getTraceLog(), "")));

        vo.setDetectionTime(formatDateTime(detectionRecord == null ? null : detectionRecord.getDetectionTime()));
        vo.setDetectorName(StrUtil.blankToDefault(detectionRecord == null ? null : detectionRecord.getDetectorName(), "-"));
        vo.setDetectionResultLabel(StrUtil.blankToDefault(LabWorkflowConstants.getDetectionResultLabel(detectionRecord == null ? null : detectionRecord.getDetectionResult()), "-"));
        vo.setDetectionStatusLabel(StrUtil.blankToDefault(LabWorkflowConstants.getDetectionStatusLabel(detectionRecord == null ? null : detectionRecord.getDetectionStatus()), "-"));
        vo.setRecordRemark(translateWorkflowText(StrUtil.blankToDefault(detectionRecord == null ? null : detectionRecord.getAbnormalRemark(), "-")));

        vo.setReviewTime(formatDateTime(latestReview == null ? null : latestReview.getReviewTime()));
        vo.setReviewerName(StrUtil.blankToDefault(latestReview == null ? null : latestReview.getReviewerName(), "-"));
        vo.setReviewResultLabel(StrUtil.blankToDefault(LabWorkflowConstants.getReviewResultLabel(latestReview == null ? null : latestReview.getReviewResult()), "-"));
        vo.setReviewRemark(translateWorkflowText(StrUtil.blankToDefault(latestReview == null ? null : latestReview.getReviewRemark(), "-")));
        vo.setRejectReason(translateWorkflowText(StrUtil.blankToDefault(latestReview == null ? null : latestReview.getRejectReason(), "-")));

        List<DetectionItem> items = detectionItems == null ? Collections.emptyList() : detectionItems;
        int abnormalCount = (int) items.stream().filter(item -> Integer.valueOf(1).equals(item.getExceedFlag())).count();
        vo.setParameterCount(items.size());
        vo.setAbnormalCount(abnormalCount);
        vo.setNormalCount(Math.max(0, items.size() - abnormalCount));
        vo.setItems(items.stream().map(this::toPreviewItem).collect(java.util.stream.Collectors.toList()));
        return vo;
    }

    private ReportPreviewItemVO toPreviewItem(DetectionItem item) {
        ReportPreviewItemVO vo = new ReportPreviewItemVO();
        vo.setParameterName(StrUtil.blankToDefault(item == null ? null : item.getParameterName(), "-"));
        vo.setMethodName(StrUtil.blankToDefault(item == null ? null : item.getMethodName(), "-"));
        vo.setUnit(StrUtil.blankToDefault(item == null ? null : item.getUnit(), "-"));
        vo.setStandardRange(item == null ? "-" : formatStandardRange(item.getStandardMin(), item.getStandardMax(), item.getUnit()));
        vo.setReferenceStandard(StrUtil.blankToDefault(item == null ? null : item.getReferenceStandard(), "-"));
        vo.setResultValue(item == null ? "-" : formatDecimal(item.getResultValue()));
        vo.setCompareText(item == null ? "-" : buildCompareText(item));
        vo.setJudgmentLabel(item != null && Integer.valueOf(1).equals(item.getExceedFlag()) ? "异常" : "正常");
        vo.setItemStatusLabel(getItemStatusLabel(item == null ? null : item.getItemStatus()));
        return vo;
    }

    private List<DetectionItem> loadDetectionItems(Long detectionRecordId) {
        if (detectionRecordId == null) {
            return Collections.emptyList();
        }
        return detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getRecordId, detectionRecordId)
                .orderByAsc(DetectionItem::getCreatedTime));
    }

    private ReviewRecord loadLatestReview(Long sampleId) {
        if (sampleId == null) {
            return null;
        }
        return reviewRecordMapper.selectOne(new LambdaQueryWrapper<ReviewRecord>()
                .eq(ReviewRecord::getSampleId, sampleId)
                .orderByDesc(ReviewRecord::getReviewTime)
                .orderByDesc(ReviewRecord::getCreatedTime)
                .last("limit 1"));
    }

    private String buildSummaryItem(String label, String value) {
        return "<div class=\"summary-item\"><span>" + escapeHtml(label) + "</span><strong>"
                + escapeHtml(StrUtil.blankToDefault(value, "-"))
                + "</strong></div>";
    }

    private String infoRow(String label1, String value1, String label2, String value2) {
        return "<tr><td class=\"label\">" + escapeHtml(label1) + "</td><td>" + escapeHtml(StrUtil.blankToDefault(value1, "-"))
                + "</td><td class=\"label\">" + escapeHtml(label2) + "</td><td>" + escapeHtml(StrUtil.blankToDefault(value2, "-"))
                + "</td></tr>";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.toString().replace('T', ' ');
    }

    private String formatDecimal(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private String formatStandardRange(BigDecimal min, BigDecimal max, String unit) {
        String suffix = StrUtil.isBlank(unit) ? "" : " " + unit;
        if (min != null && max != null) {
            return formatDecimal(min) + " - " + formatDecimal(max) + suffix;
        }
        if (min != null) {
            return ">= " + formatDecimal(min) + suffix;
        }
        if (max != null) {
            return "<= " + formatDecimal(max) + suffix;
        }
        return "未设置";
    }

    private String buildCompareText(DetectionItem item) {
        if (item == null || item.getResultValue() == null) {
            return "未录入检测值";
        }
        BigDecimal value = item.getResultValue();
        if (item.getStandardMin() != null && value.compareTo(item.getStandardMin()) < 0) {
            return "低于标准下限：" + formatDecimal(item.getStandardMin());
        }
        if (item.getStandardMax() != null && value.compareTo(item.getStandardMax()) > 0) {
            return "高于标准上限：" + formatDecimal(item.getStandardMax());
        }
        if (item.getStandardMin() != null || item.getStandardMax() != null) {
            return "处于标准范围内";
        }
        return "未设置判定范围，无法自动判断";
    }

    private String resolveReportTypeLabel(String reportType) {
        if (LabWorkflowConstants.ReportType.DAILY.equals(reportType)) {
            return "日报";
        }
        if (LabWorkflowConstants.ReportType.WEEKLY.equals(reportType)) {
            return "周报";
        }
        if (LabWorkflowConstants.ReportType.MONTHLY.equals(reportType)) {
            return "月报";
        }
        return StrUtil.blankToDefault(reportType, "-");
    }

    private String renderTag(String text, String type) {
        return "<span class=\"tag tag-" + escapeHtml(type) + "\">" + escapeHtml(StrUtil.blankToDefault(text, "-")) + "</span>";
    }

    private String getItemStatusLabel(String status) {
        return StrUtil.blankToDefault(LabWorkflowConstants.getDetectionStatusLabel(status), "-");
    }

    private String mapItemStatusTag(String status) {
        if (LabWorkflowConstants.DetectionStatus.APPROVED.equals(status)) {
            return "success";
        }
        if (LabWorkflowConstants.DetectionStatus.REJECTED.equals(status)) {
            return "danger";
        }
        if (LabWorkflowConstants.DetectionStatus.SUBMITTED.equals(status)) {
            return "warning";
        }
        return "info";
    }

    private String escapeHtml(String text) {
        return StrUtil.blankToDefault(text, "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private void refreshStoredContentSnapshotIfNeeded(LabReport report) {
        if (report == null) {
            return;
        }
        String translatedSnapshot = translateWorkflowText(StrUtil.blankToDefault(report.getContentSnapshot(), ""));
        if (StrUtil.equals(translatedSnapshot, StrUtil.blankToDefault(report.getContentSnapshot(), ""))) {
            return;
        }
        report.setContentSnapshot(translatedSnapshot);
        LabReport update = new LabReport();
        update.setId(report.getId());
        update.setContentSnapshot(translatedSnapshot);
        labReportMapper.updateById(update);
    }

    private void refreshStoredPresentation(LabReport report, boolean refreshArtifact) {
        if (report == null) {
            return;
        }

        boolean changed = false;
        String translatedSnapshot = translateWorkflowText(StrUtil.blankToDefault(report.getContentSnapshot(), ""));
        if (!StrUtil.equals(translatedSnapshot, StrUtil.blankToDefault(report.getContentSnapshot(), ""))) {
            report.setContentSnapshot(translatedSnapshot);
            changed = true;
        }

        if (refreshArtifact) {
            LabSample sample = report.getSampleId() == null ? null : labSampleMapper.selectById(report.getSampleId());
            DetectionRecord detectionRecord = report.getDetectionRecordId() == null
                    ? null
                    : detectionRecordMapper.selectById(report.getDetectionRecordId());
            List<DetectionItem> detectionItems = loadDetectionItems(report.getDetectionRecordId());
            ReviewRecord latestReview = loadLatestReview(report.getSampleId());
            String filePath = writeReportArtifact(report, sample, detectionRecord, detectionItems, latestReview);
            if (!StrUtil.equals(filePath, report.getFilePath())) {
                report.setFilePath(filePath);
                changed = true;
            }
        }

        if (!changed) {
            return;
        }

        LabReport update = new LabReport();
        update.setId(report.getId());
        update.setContentSnapshot(report.getContentSnapshot());
        if (refreshArtifact) {
            update.setFilePath(report.getFilePath());
        }
        labReportMapper.updateById(update);
    }

    private String translateWorkflowText(String text) {
        if (StrUtil.isBlank(text)) {
            return text;
        }
        String result = text;
        result = result.replaceAll("\\bFACTORY\\b", "出厂水");
        result = result.replaceAll("\\bRAW\\b", "原水");
        result = result.replaceAll("\\bTERMINAL\\b", "末梢水");
        result = result.replaceAll("\\bLOGGED\\b", "已登记");
        result = result.replaceAll("\\bREVIEWING\\b", "审核中");
        result = result.replaceAll("\\bRETEST\\b", "退回重检");
        result = result.replaceAll("\\bCOMPLETED\\b", "已完成");
        result = result.replaceAll("\\bWAIT_ASSIGN\\b", "待分配");
        result = result.replaceAll("\\bWAIT_DETECT\\b", "待检测");
        result = result.replaceAll("\\bSUBMITTED\\b", "已提交");
        result = result.replaceAll("\\bAPPROVED\\b", "已通过");
        result = result.replaceAll("\\bREJECTED\\b", "已驳回");
        result = result.replaceAll("\\bABNORMAL\\b", "异常");
        result = result.replaceAll("\\bNORMAL\\b", "正常");
        result = result.replaceAll("\\bDRAFT\\b", "草稿");
        result = result.replaceAll("\\bGENERATED\\b", "已生成");
        result = result.replaceAll("\\bPUBLISHED\\b", "已发布");
        result = result.replaceAll("\\bPENDING\\b", "待推送");
        result = result.replaceAll("\\bSUCCESS\\b", "成功");
        result = result.replaceAll("\\bFAILED\\b", "失败");
        result = result.replaceAll("\\bCANCELLED\\b", "已取消");
        return result;
    }

    private PushResult pushReport(LabReport report, LocalDateTime pushTime) {
        List<PushRecipient> recipients = resolvePushRecipients(report);
        if (recipients.isEmpty()) {
            return new PushResult("PENDING", "未配置报告接收人，暂未执行推送");
        }
        List<String> recipientNames = new ArrayList<>();
        for (PushRecipient recipient : recipients) {
            ReportPushRecord pushRecord = new ReportPushRecord();
            pushRecord.setReportId(report.getId());
            pushRecord.setSampleId(report.getSampleId());
            pushRecord.setSampleNo(report.getSampleNo());
            pushRecord.setSealNo(report.getSealNo());
            pushRecord.setRecipientUserId(recipient.userId);
            pushRecord.setRecipientName(recipient.recipientName);
            pushRecord.setRecipientPhone(recipient.recipientPhone);
            pushRecord.setPushChannel("INTERNAL");
            pushRecord.setPushStatus("SUCCESS");
            pushRecord.setPushMessage("报告已推送到接收人：" + report.getReportName());
            pushRecord.setPushTime(pushTime);
            reportPushRecordMapper.insert(pushRecord);
            recipientNames.add(recipient.recipientName);
        }
        return new PushResult("SUCCESS",
                "成功推送给" + recipientNames.size() + "位接收人：" + String.join("、", recipientNames));
    }

    private List<PushRecipient> resolvePushRecipients(LabReport report) {
        Map<String, PushRecipient> recipientMap = new LinkedHashMap<>();
        LabSample sample = report.getSampleId() == null ? null : labSampleMapper.selectById(report.getSampleId());
        if (sample != null && sample.getSamplerId() != null) {
            addRecipient(recipientMap, sample.getSamplerId(), sample.getSamplerName());
        }
        if (report.getSampleId() == null) {
            return new ArrayList<>(recipientMap.values());
        }
        ReviewRecord latestReview = reviewRecordMapper.selectOne(new LambdaQueryWrapper<ReviewRecord>()
                .eq(ReviewRecord::getSampleId, report.getSampleId())
                .orderByDesc(ReviewRecord::getReviewTime)
                .orderByDesc(ReviewRecord::getCreatedTime)
                .last("limit 1"));
        if (latestReview != null && latestReview.getReviewerId() != null) {
            addRecipient(recipientMap, latestReview.getReviewerId(), latestReview.getReviewerName());
        }
        return new ArrayList<>(recipientMap.values());
    }

    private void addRecipient(Map<String, PushRecipient> recipientMap, Long userId, String fallbackName) {
        if (userId == null) {
            return;
        }
        LabUser user = labUserMapper.selectById(userId);
        String recipientName = user == null ? StrUtil.blankToDefault(fallbackName, "未命名接收人") : StrUtil.blankToDefault(user.getRealName(), fallbackName);
        String recipientPhone = user == null ? null : user.getPhone();
        recipientMap.putIfAbsent(String.valueOf(userId), new PushRecipient(userId, recipientName, recipientPhone));
    }

    private static class PushRecipient {

        private final Long userId;

        private final String recipientName;

        private final String recipientPhone;

        private PushRecipient(Long userId, String recipientName, String recipientPhone) {
            this.userId = userId;
            this.recipientName = recipientName;
            this.recipientPhone = recipientPhone;
        }
    }

    private static class PushResult {

        private final String pushStatus;

        private final String pushMessage;

        private PushResult(String pushStatus, String pushMessage) {
            this.pushStatus = pushStatus;
            this.pushMessage = pushMessage;
        }
    }
}
