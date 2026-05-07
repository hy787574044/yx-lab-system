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
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.report.dto.ReportQuery;
import com.yx.lab.modules.report.dto.ReportTemplateSaveCommand;
import com.yx.lab.modules.report.entity.LabReport;
import com.yx.lab.modules.report.entity.ReportPushRecord;
import com.yx.lab.modules.report.entity.ReportTemplate;
import com.yx.lab.modules.report.mapper.LabReportMapper;
import com.yx.lab.modules.report.mapper.ReportPushRecordMapper;
import com.yx.lab.modules.report.mapper.ReportTemplateMapper;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final LabReportMapper labReportMapper;

    private final ReportTemplateMapper reportTemplateMapper;

    private final LabSampleService labSampleService;

    private final LabSampleMapper labSampleMapper;

    private final ReviewRecordMapper reviewRecordMapper;

    private final LabUserMapper labUserMapper;

    private final ReportPushRecordMapper reportPushRecordMapper;

    private final StorageService storageService;

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
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public PageResult<ReportTemplate> templatePage(com.yx.lab.common.model.PageQuery query) {
        Page<ReportTemplate> page = reportTemplateMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<ReportTemplate>().orderByDesc(ReportTemplate::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public void saveTemplate(ReportTemplateSaveCommand command) {
        ReportTemplate template = new ReportTemplate();
        applyTemplateCommand(template, command);
        reportTemplateMapper.insert(template);
    }

    public void updateTemplate(Long id, ReportTemplateSaveCommand command) {
        ReportTemplate template = requireTemplate(id);
        applyTemplateCommand(template, command);
        reportTemplateMapper.updateById(template);
    }

    public void deleteTemplate(Long id) {
        reportTemplateMapper.deleteById(requireTemplate(id).getId());
    }

    public void publish(Long id) {
        LabReport existing = requireReport(id);
        if (!LabWorkflowConstants.canPublishReport(existing.getReportStatus())) {
            throw new BusinessException("当前报告状态不允许发布");
        }
        ensureReportArtifact(existing);

        CurrentUser currentUser = requireCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        LabReport report = new LabReport();
        report.setId(existing.getId());
        report.setReportStatus(LabWorkflowConstants.ReportStatus.PUBLISHED);
        report.setPublishedTime(now);
        report.setPublishedBy(currentUser.getUserId());
        report.setPublishedByName(currentUser.getRealName());

        PushResult pushResult = pushReport(existing, now);
        report.setPushStatus(pushResult.pushStatus);
        report.setLastPushTime(now);
        report.setLastPushMessage(pushResult.pushMessage);
        labReportMapper.updateById(report);

        if (existing.getSampleId() != null) {
            labSampleService.appendTrace(existing.getSampleId(),
                    "报告发布：报告名称=" + existing.getReportName()
                            + "，发布人=" + currentUser.getRealName()
                            + "，推送结果=" + pushResult.pushMessage);
        }
    }

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
                    "报告撤回：报告名称=" + existing.getReportName() + "，状态=已取消发布");
        }
    }

    public void createApprovedReport(LabSample sample, DetectionRecord record) {
        ReportTemplate template = reportTemplateMapper.selectOne(new LambdaQueryWrapper<ReportTemplate>()
                .eq(ReportTemplate::getDefaultTemplate, 1)
                .last("limit 1"));

        String content;
        if (template == null) {
            content = "样品编号：" + sample.getSampleNo()
                    + "\n封签编号：" + sample.getSealNo()
                    + "\n点位名称：" + sample.getPointName()
                    + "\n检测类型：" + record.getDetectionTypeName()
                    + "\n检测结果：" + record.getDetectionResult();
        } else {
            content = template.getTemplateContent()
                    .replace("${sampleNo}", sample.getSampleNo())
                    .replace("${sealNo}", StrUtil.blankToDefault(sample.getSealNo(), "-"))
                    .replace("${pointName}", sample.getPointName())
                    .replace("${detectionType}", record.getDetectionTypeName())
                    .replace("${detectionResult}", record.getDetectionResult());
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
        report.setFilePath(writeReportArtifact(sample, report.getReportName(), content));
        labReportMapper.insert(report);
        labSampleService.appendTrace(sample.getId(),
                "报告生成：封签号=" + sample.getSealNo()
                        + "，报告名称=" + report.getReportName()
                        + "，状态=已生成");
    }

    public byte[] preview(Long id) {
        LabReport report = requireReport(id);
        ensureReportArtifact(report);
        try {
            return storageService.readAllBytes(report.getFilePath());
        } catch (IOException ex) {
            throw new BusinessException("报告文件读取失败：" + ex.getMessage());
        }
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
            throw new BusinessException("当前登录信息已失效，请重新登录");
        }
        return currentUser;
    }

    private void ensureReportArtifact(LabReport report) {
        if (report == null) {
            return;
        }
        if (StrUtil.isNotBlank(report.getFilePath())) {
            return;
        }
        LabSample sample = report.getSampleId() == null ? null : labSampleMapper.selectById(report.getSampleId());
        String reportName = StrUtil.blankToDefault(report.getReportName(), report.getSampleNo() + "-检测报告");
        report.setFilePath(writeReportArtifact(sample, reportName, report.getContentSnapshot()));
        labReportMapper.updateById(report);
    }

    private String writeReportArtifact(LabSample sample, String reportName, String content) {
        String safeSampleNo = sample == null ? "report" : StrUtil.blankToDefault(sample.getSampleNo(), "report");
        String fileName = "reports/" + safeSampleNo + "-" + System.currentTimeMillis() + ".html";
        try {
            return storageService.storeText(fileName, buildReportHtml(sample, reportName, content));
        } catch (IOException ex) {
            throw new BusinessException("报告文件生成失败：" + ex.getMessage());
        }
    }

    private String buildReportHtml(LabSample sample, String reportName, String content) {
        String sampleNo = sample == null ? "-" : StrUtil.blankToDefault(sample.getSampleNo(), "-");
        String sealNo = sample == null ? "-" : StrUtil.blankToDefault(sample.getSealNo(), "-");
        String pointName = sample == null ? "-" : StrUtil.blankToDefault(sample.getPointName(), "-");
        String samplingTime = sample == null || sample.getSamplingTime() == null ? "-" : sample.getSamplingTime().toString().replace('T', ' ');
        String reportBody = escapeHtml(StrUtil.blankToDefault(content, "-")).replace("\n", "<br/>");
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>" + escapeHtml(reportName) + "</title>"
                + "<style>body{font-family:SimSun,Microsoft YaHei,sans-serif;padding:32px;color:#1f2937;}h1{font-size:24px;margin-bottom:16px;}table{border-collapse:collapse;width:100%;margin-bottom:24px;}td{border:1px solid #d1d5db;padding:10px;}section{line-height:1.8;}</style>"
                + "</head><body><h1>" + escapeHtml(reportName) + "</h1>"
                + "<table><tr><td>样品编号</td><td>" + escapeHtml(sampleNo) + "</td><td>封签编号</td><td>" + escapeHtml(sealNo) + "</td></tr>"
                + "<tr><td>点位名称</td><td>" + escapeHtml(pointName) + "</td><td>采样时间</td><td>" + escapeHtml(samplingTime) + "</td></tr></table>"
                + "<section>" + reportBody + "</section></body></html>";
    }

    private String escapeHtml(String text) {
        return StrUtil.blankToDefault(text, "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private PushResult pushReport(LabReport report, LocalDateTime pushTime) {
        List<PushRecipient> recipients = resolvePushRecipients(report);
        if (recipients.isEmpty()) {
            return new PushResult("PENDING", "未找到可推送接收人，已保留正式报告产物");
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
            pushRecord.setPushMessage("报告已发布，可查看正式文件：" + report.getReportName());
            pushRecord.setPushTime(pushTime);
            reportPushRecordMapper.insert(pushRecord);
            recipientNames.add(recipient.recipientName);
        }
        return new PushResult("SUCCESS",
                "已推送给" + recipientNames.size() + "人：" + String.join("、", recipientNames));
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
        String recipientName = user == null ? StrUtil.blankToDefault(fallbackName, "未知接收人") : StrUtil.blankToDefault(user.getRealName(), fallbackName);
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
