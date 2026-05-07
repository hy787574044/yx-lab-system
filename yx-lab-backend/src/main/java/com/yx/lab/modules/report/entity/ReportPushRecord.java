package com.yx.lab.modules.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("lab_report_push_record")
@EqualsAndHashCode(callSuper = true)
public class ReportPushRecord extends BaseEntity {

    private Long reportId;

    private Long sampleId;

    private String sampleNo;

    private String sealNo;

    private Long recipientUserId;

    private String recipientName;

    private String recipientPhone;

    private String pushChannel;

    private String pushStatus;

    private String pushMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pushTime;
}
