package com.yx.lab.modules.sample.entity;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 样品实体，记录样品登录后的主档信息及流程留痕。
 */
@Data
@TableName("lab_sample")
@EqualsAndHashCode(callSuper = true)
public class LabSample extends BaseEntity {

    @Schema(description = "样品编号")
    @TableField("sample_no")
    private String sampleNo;

    @Schema(description = "采样任务ID")
    @TableField("task_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;

    @Schema(description = "所属机构ID")
    @TableField("org_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orgId;

    @Schema(description = "监测点位ID")
    @TableField("point_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long pointId;

    @Schema(description = "监测点位名称")
    @TableField("point_name")
    private String pointName;

    @Schema(description = "样品类型")
    @TableField("sample_type")
    private String sampleType;

    @Schema(description = "样品来源")
    @TableField("sample_source_method")
    private String sampleSourceMethod;


    @Schema(description = "检测项目文本")
    @TableField("detection_items")
    private String detectionItems;

    @Schema(description = "检测套餐ID")
    @TableField("detection_type_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long detectionTypeId;

    @Schema(description = "检测套餐名称")
    @TableField("detection_type_name")
    private String detectionTypeName;

    @Schema(description = "检测配置快照")
    @TableField("detection_config_snapshot")
    private String detectionConfigSnapshot;

    @Schema(description = "审核流程ID")
    @TableField("review_flow_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long reviewFlowId;

    @Schema(description = "审核流程名称")
    @TableField("review_flow_name")
    private String reviewFlowName;



    @Schema(description = "采样时间")
    @TableField("sampling_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime samplingTime;

    @Schema(description = "采样员ID")
    @TableField("sampler_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long samplerId;

    @Schema(description = "采样员名称")
    @TableField("sampler_name")
    private String samplerName;

    @Schema(description = "天气情况")
    @TableField("weather")
    private String weather;

    @Schema(description = "保存条件")
    @TableField("storage_condition")
    private String storageCondition;

    @Schema(description = "样品状态")
    @TableField("sample_status")
    private String sampleStatus;

    @Schema(description = "结果摘要")
    @TableField("result_summary")
    private String resultSummary;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    @Schema(description = "流程留痕")
    @TableField("trace_log")
    private String traceLog;

    @Schema(description = "采样总容量")
    @TableField("sample_total_volume")
    private String sampleTotalVolume;

    @Schema(description = "采样瓶数")
    @TableField("sample_bottle_count")
    private String sampleBottleCount;
}
