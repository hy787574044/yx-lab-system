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
 * 报告推送记录实体，记录报告向外部或内部用户推送的留痕信息。
 */
@Data
@TableName("lab_report_push_record")
@EqualsAndHashCode(callSuper = true)
public class ReportPushRecord extends BaseEntity {

    @Schema(description = "报告ID")
    @TableField("report_id")
    private Long reportId;

    @Schema(description = "样品ID")
    @TableField("sample_id")
    private Long sampleId;

    @Schema(description = "样品编号")
    @TableField("sample_no")
    private String sampleNo;

    @Schema(description = "封签编号")
    @TableField("seal_no")
    private String sealNo;

    @Schema(description = "接收人用户ID")
    @TableField("recipient_user_id")
    private Long recipientUserId;

    @Schema(description = "接收人名称")
    @TableField("recipient_name")
    private String recipientName;

    @Schema(description = "接收人手机号")
    @TableField("recipient_phone")
    private String recipientPhone;

    @Schema(description = "推送渠道")
    @TableField("push_channel")
    private String pushChannel;

    @Schema(description = "推送状态")
    @TableField("push_status")
    private String pushStatus;

    @Schema(description = "推送说明")
    @TableField("push_message")
    private String pushMessage;

    @Schema(description = "推送时间")
    @TableField("push_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pushTime;
}
