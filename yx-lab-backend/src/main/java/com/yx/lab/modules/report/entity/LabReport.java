package com.yx.lab.modules.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("lab_report")
@EqualsAndHashCode(callSuper = true)
public class LabReport extends BaseEntity {

    private String reportName;

    private String reportType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedTime;

    private Long sampleId;

    private String sampleNo;

    private String sealNo;

    private Long detectionRecordId;

    private String reportStatus;

    private String filePath;

    private String contentSnapshot;
}
