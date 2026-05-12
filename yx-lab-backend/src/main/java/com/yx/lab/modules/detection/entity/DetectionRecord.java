package com.yx.lab.modules.detection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 检测主流程实体，对应一个样品的一次完整检测流程记录。
 */
@Data
@TableName("lab_detection_record")
@EqualsAndHashCode(callSuper = true)
public class DetectionRecord extends BaseEntity {

    @Schema(description = "样品ID")
    @TableField("sample_id")
    private Long sampleId;

    @Schema(description = "样品编号")
    @TableField("sample_no")
    private String sampleNo;

    @Schema(description = "封签编号")
    @TableField("seal_no")
    private String sealNo;

    @Schema(description = "检测套餐ID")
    @TableField("detection_type_id")
    private Long detectionTypeId;

    @Schema(description = "检测套餐名称")
    @TableField("detection_type_name")
    private String detectionTypeName;

    @Schema(description = "检测时间")
    @TableField("detection_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime detectionTime;

    @Schema(description = "检测员ID")
    @TableField("detector_id")
    private Long detectorId;

    @Schema(description = "检测员名称")
    @TableField("detector_name")
    private String detectorName;

    @Schema(description = "综合检测结果")
    @TableField("detection_result")
    private String detectionResult;

    @Schema(description = "异常说明")
    @TableField("abnormal_remark")
    private String abnormalRemark;

    @Schema(description = "检测流程状态")
    @TableField("detection_status")
    private String detectionStatus;

    @Schema(description = "参数总数")
    @TableField(exist = false)
    private Integer parameterCount;

    @Schema(description = "已分配数量")
    @TableField(exist = false)
    private Integer assignedCount;

    @Schema(description = "已完成数量")
    @TableField(exist = false)
    private Integer completedCount;
}
