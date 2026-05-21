package com.yx.lab.modules.review.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 结果审核实体，记录检测流程的审核结论和驳回原因。
 */
@Data
@TableName("lab_review_record")
@EqualsAndHashCode(callSuper = true)
public class ReviewRecord extends BaseEntity {

    @Schema(description = "检测主流程ID")
    @TableField("detection_record_id")
    private Long detectionRecordId;

    @Schema(description = "样品ID")
    @TableField("sample_id")
    private Long sampleId;

    @Schema(description = "样品编号")
    @TableField("sample_no")
    private String sampleNo;

    @Schema(description = "封签编号")
    @TableField("seal_no")
    private String sealNo;

    @Schema(description = "审核流程ID")
    @TableField("flow_id")
    private Long flowId;

    @Schema(description = "审核流程节点ID")
    @TableField("flow_node_id")
    private Long flowNodeId;

    @Schema(description = "审核流程节点名称")
    @TableField("flow_node_name")
    private String flowNodeName;

    @Schema(description = "审核流程节点顺序")
    @TableField("flow_node_order")
    private Integer flowNodeOrder;

    @Schema(description = "是否必审，1是，0否")
    @TableField("required_flag")
    private Integer requiredFlag;

    @Schema(description = "检测套餐名称")
    @TableField(exist = false)
    private String detectionTypeName;

    @Schema(description = "检测人员")
    @TableField(exist = false)
    private String detectorName;

    @Schema(description = "参数总数")
    @TableField(exist = false)
    private Integer parameterCount;

    @Schema(description = "已完成数量")
    @TableField(exist = false)
    private Integer completedCount;

    @Schema(description = "审核人ID")
    @TableField("reviewer_id")
    private Long reviewerId;

    @Schema(description = "审核人名称")
    @TableField("reviewer_name")
    private String reviewerName;

    @Schema(description = "审核时间")
    @TableField("review_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;

    @Schema(description = "审核结果")
    @TableField("review_result")
    private String reviewResult;

    @Schema(description = "驳回原因")
    @TableField("reject_reason")
    private String rejectReason;

    @Schema(description = "审核意见")
    @TableField("review_remark")
    private String reviewRemark;
}
