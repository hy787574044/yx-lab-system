package com.yx.lab.modules.sample.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 采样任务实体，记录由计划派发后的现场采样执行任务。
 */
@Data
@TableName("lab_sampling_task")
@EqualsAndHashCode(callSuper = true)
public class SamplingTask extends BaseEntity {

    @Schema(description = "任务编号")
    @TableField("task_no")
    private String taskNo;

    @Schema(description = "采样计划ID")
    @TableField("plan_id")
    private Long planId;

    @Schema(description = "监测点位ID")
    @TableField("point_id")
    private Long pointId;

    @Schema(description = "监测点位名称")
    @TableField("point_name")
    private String pointName;

    @Schema(description = "计划采样时间")
    @TableField("sampling_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime samplingTime;

    @Schema(description = "采样员ID")
    @TableField("sampler_id")
    private Long samplerId;

    @Schema(description = "采样员名称")
    @TableField("sampler_name")
    private String samplerName;

    @Schema(description = "样品类型")
    @TableField("sample_type")
    private String sampleType;

    @Schema(description = "封签编号")
    @TableField("seal_no")
    private String sealNo;

    @Schema(description = "样品登记状态")
    @TableField("sample_register_status")
    private String sampleRegisterStatus;

    @Schema(description = "样品ID")
    @TableField("sample_id")
    private Long sampleId;

    @Schema(description = "检测项目文本")
    @TableField("detection_items")
    private String detectionItems;

    @Schema(description = "任务状态")
    @TableField("task_status")
    private String taskStatus;

    @Schema(description = "开始执行时间")
    @TableField("started_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedTime;

    @Schema(description = "现场指标记录")
    @TableField("onsite_metrics")
    private String onsiteMetrics;

    @Schema(description = "现场照片地址集合")
    @TableField("photo_urls")
    private String photoUrls;

    @Schema(description = "中止原因")
    @TableField("abandon_reason")
    private String abandonReason;

    @Schema(description = "完成时间")
    @TableField("finished_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedTime;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
