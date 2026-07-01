package com.yx.lab.modules.sample.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("lab_sampling_task")
@EqualsAndHashCode(callSuper = true)
public class SamplingTask extends BaseEntity {

    @Schema(description = "任务编号")
    @TableField("task_no")
    private String taskNo;

    @Schema(description = "样品编号")
    @TableField("sample_no")
    private String sampleNo;

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

    @Schema(description = "采样员ID集合")
    @TableField("sampler_ids")
    private String samplerIds;

    @Schema(description = "采样员名称")
    @TableField("sampler_name")
    private String samplerName;

    @Schema(description = "样品类型")
    @TableField("sample_type")
    private String sampleType;

    @Schema(description = "样品登记状态")
    @TableField("sample_register_status")
    private String sampleRegisterStatus;

    @Schema(description = "样品ID")
    @TableField("sample_id")
    private Long sampleId;

    @Schema(description = "检测项目文本")
    @TableField("detection_items")
    private String detectionItems;

    @Schema(description = "检测套餐ID")
    @TableField("detection_type_id")
    private Long detectionTypeId;

    @Schema(description = "检测套餐名称")
    @TableField("detection_type_name")
    private String detectionTypeName;

    @Schema(description = "检测配置快照")
    @TableField("detection_config_snapshot")
    private String detectionConfigSnapshot;

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

    @Schema(description = "现场天气情况")
    @TableField("weather")
    private String weather;

    @Schema(description = "现场温度")
    @TableField("temperature")
    private String temperature;

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

    @Schema(description = "坐标点名称")
    @TableField("address")
    private String address;

    @Schema(description = "X坐标")
    @TableField("latitude")
    private String latitude;

    @Schema(description = "Y坐标")
    @TableField("longitude")
    private String longitude;

    @Schema(description = "采样总容量")
    @TableField("sample_total_volume")
    private String sampleTotalVolume;

    @Schema(description = "采样瓶数")
    @TableField("sample_bottle_count")
    private String sampleBottleCount;

    @Schema(description = "计划名称（不从数据库映射，仅用于视图展示）")
    @TableField(exist = false)
    private String planName;
}
