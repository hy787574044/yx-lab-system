package com.yx.lab.modules.report.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.report.dto.ReportQuery;
import com.yx.lab.modules.report.entity.LabReport;
import com.yx.lab.modules.report.entity.ReportTemplate;
import com.yx.lab.modules.report.mapper.LabReportMapper;
import com.yx.lab.modules.report.mapper.ReportTemplateMapper;
import com.yx.lab.modules.sample.entity.LabSample;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final LabReportMapper labReportMapper;

    private final ReportTemplateMapper reportTemplateMapper;

    public PageResult<LabReport> page(ReportQuery query) {
        Page<LabReport> page = labReportMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<LabReport>()
                        .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                                .like(LabReport::getReportName, query.getKeyword())
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

    public void saveTemplate(ReportTemplate template) {
        reportTemplateMapper.insert(template);
    }

    public void updateTemplate(ReportTemplate template) {
        reportTemplateMapper.updateById(template);
    }

    public void deleteTemplate(Long id) {
        reportTemplateMapper.deleteById(id);
    }

    public void publish(Long id) {
        LabReport report = new LabReport();
        report.setId(id);
        report.setReportStatus("PUBLISHED");
        labReportMapper.updateById(report);
    }

    public void unpublish(Long id) {
        LabReport report = new LabReport();
        report.setId(id);
        report.setReportStatus("DRAFT");
        labReportMapper.updateById(report);
    }

    public void createApprovedReport(LabSample sample, DetectionRecord record) {
        ReportTemplate template = reportTemplateMapper.selectOne(new LambdaQueryWrapper<ReportTemplate>()
                .eq(ReportTemplate::getDefaultTemplate, 1)
                .last("limit 1"));
        String content = template == null
                ? "样品编号：" + sample.getSampleNo() + "\n检测类型：" + record.getDetectionTypeName() + "\n检测结果：" + record.getDetectionResult()
                : template.getTemplateContent()
                .replace("${sampleNo}", sample.getSampleNo())
                .replace("${pointName}", sample.getPointName())
                .replace("${detectionType}", record.getDetectionTypeName())
                .replace("${detectionResult}", record.getDetectionResult());
        LabReport report = new LabReport();
        report.setReportName(sample.getSampleNo() + "-检测报告");
        report.setReportType("DAILY");
        report.setGeneratedTime(LocalDateTime.now());
        report.setSampleId(sample.getId());
        report.setSampleNo(sample.getSampleNo());
        report.setDetectionRecordId(record.getId());
        report.setReportStatus("DRAFT");
        report.setContentSnapshot(content);
        labReportMapper.insert(report);
    }
}
