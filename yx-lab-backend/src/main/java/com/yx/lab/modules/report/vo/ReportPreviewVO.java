package com.yx.lab.modules.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 报告预览结构化数据。
 */
@Data
@Schema(description = "报告预览结构化数据")
public class ReportPreviewVO {

    @Schema(description = "报告主键")
    private Long reportId;

    @Schema(description = "报告名称")
    private String reportName;

    @Schema(description = "报告类型中文")
    private String reportTypeLabel;

    @Schema(description = "报告状态中文")
    private String reportStatusLabel;

    @Schema(description = "生成时间")
    private String generatedTime;

    @Schema(description = "发布时间")
    private String publishedTime;

    @Schema(description = "发布人")
    private String publishedByName;

    @Schema(description = "推送状态中文")
    private String pushStatusLabel;

    @Schema(description = "最近推送时间")
    private String lastPushTime;

    @Schema(description = "推送结果说明")
    private String lastPushMessage;

    @Schema(description = "样品编号")
    private String sampleNo;

    @Schema(description = "封签编号")
    private String sealNo;

    @Schema(description = "点位名称")
    private String pointName;

    @Schema(description = "样品类型中文")
    private String sampleTypeLabel;

    @Schema(description = "采样时间")
    private String samplingTime;

    @Schema(description = "封签时间")
    private String sealTime;

    @Schema(description = "采样人员")
    private String samplerName;

    @Schema(description = "天气情况")
    private String weather;

    @Schema(description = "保存条件")
    private String storageCondition;

    @Schema(description = "样品状态中文")
    private String sampleStatusLabel;

    @Schema(description = "结果摘要")
    private String resultSummary;

    @Schema(description = "样品备注")
    private String sampleRemark;

    @Schema(description = "流程留痕")
    private String traceLog;

    @Schema(description = "化验完成时间")
    private String detectionTime;

    @Schema(description = "化验人员")
    private String detectorName;

    @Schema(description = "综合化验结果中文")
    private String detectionResultLabel;

    @Schema(description = "检测流程状态中文")
    private String detectionStatusLabel;

    @Schema(description = "流程说明")
    private String recordRemark;

    @Schema(description = "审查时间")
    private String reviewTime;

    @Schema(description = "审查人员")
    private String reviewerName;

    @Schema(description = "审查结论中文")
    private String reviewResultLabel;

    @Schema(description = "审查意见")
    private String reviewRemark;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "参数总数")
    private Integer parameterCount;

    @Schema(description = "正常项数量")
    private Integer normalCount;

    @Schema(description = "异常项数量")
    private Integer abnormalCount;

    @Schema(description = "明细列表")
    private List<ReportPreviewItemVO> items = new ArrayList<>();
}
