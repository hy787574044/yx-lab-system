package com.yx.lab.modules.detection.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 检测子流程实体，对应单个样品下某一检测参数的检测执行记录。
 */
@Data
@TableName("lab_detection_item")
@EqualsAndHashCode(callSuper = true)
public class DetectionItem extends BaseEntity {

    @Schema(description = "检测主流程ID")
    @TableField("record_id")
    private Long recordId;

    @Schema(description = "检测参数ID")
    @TableField("parameter_id")
    private Long parameterId;

    @Schema(description = "检测参数名称")
    @TableField("parameter_name")
    private String parameterName;

    @Schema(description = "标准下限")
    @TableField("standard_min")
    private BigDecimal standardMin;

    @Schema(description = "标准上限")
    @TableField("standard_max")
    private BigDecimal standardMax;

    @Schema(description = "检测结果值")
    @TableField("result_value")
    private BigDecimal resultValue;

    @Schema(description = "单位")
    @TableField("unit")
    private String unit;

    @Schema(description = "参考范围")
    @TableField("reference_standard")
    private String referenceStandard;

    @Schema(description = "检测方法ID")
    @TableField("method_id")
    private Long methodId;

    @Schema(description = "检测方法名称")
    @TableField("method_name")
    private String methodName;

    @Schema(description = "检测依据")
    @TableField(exist = false)
    private String methodBasis;

    @Schema(description = "检测员ID")
    @TableField("detector_id")
    private Long detectorId;

    @Schema(description = "检测员名称")
    @TableField("detector_name")
    private String detectorName;

    @Schema(description = "子流程状态")
    @TableField("item_status")
    private String itemStatus;

    @Schema(description = "超标标记，1超标，0正常")
    @TableField("exceed_flag")
    private Integer exceedFlag;
}
