package com.yx.lab.modules.report.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 报告实体，记录正式报告的生成、发布及产物信息。
 */
@Data
@TableName("lab_report")
@EqualsAndHashCode(callSuper = true)
public class LabReport extends BaseEntity {

    @Schema(description = "报告名称")
    @TableField("report_name")
    private String reportName;

    @Schema(description = "报告类型")
    @TableField("report_type")
    private String reportType;

    @Schema(description = "报告类别")
    @TableField("report_category")
    private String reportCategory;

    @Schema(description = "生成时间")
    @TableField("generated_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedTime;

    @Schema(description = "样品ID")
    @TableField("sample_id")
    private Long sampleId;

    @Schema(description = "样品编号")
    @TableField("sample_no")
    private String sampleNo;

    @Schema(description = "检测主流程ID")
    @TableField("detection_record_id")
    private Long detectionRecordId;

    @Schema(description = "报告状态")
    @TableField("report_status")
    private String reportStatus;

    @Schema(description = "发布时间")
    @TableField("published_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedTime;

    @Schema(description = "发布人ID")
    @TableField("published_by")
    private Long publishedBy;

    @Schema(description = "发布人名称")
    @TableField("published_by_name")
    private String publishedByName;

    @Schema(description = "报告文件路径")
    @TableField("file_path")
    private String filePath;

    @Schema(description = "报告内容快照")
    @TableField("content_snapshot")
    private String contentSnapshot;
}
