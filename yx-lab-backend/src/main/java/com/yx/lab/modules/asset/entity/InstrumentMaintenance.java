package com.yx.lab.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备维修记录实体，记录设备维修、保养及费用信息。
 */
@Data
@TableName("lab_instrument_maintenance")
@EqualsAndHashCode(callSuper = true)
public class InstrumentMaintenance extends BaseEntity {

    @Schema(description = "设备ID")
    @TableField("instrument_id")
    private Long instrumentId;

    @Schema(description = "设备名称")
    @TableField("instrument_name")
    private String instrumentName;

    @Schema(description = "维修时间")
    @TableField("maintenance_time")
    private LocalDateTime maintenanceTime;

    @Schema(description = "维修原因")
    @TableField("maintenance_reason")
    private String maintenanceReason;

    @Schema(description = "维修人员")
    @TableField("maintainer_name")
    private String maintainerName;

    @Schema(description = "维修单位")
    @TableField("maintenance_company")
    private String maintenanceCompany;

    @Schema(description = "维修结果")
    @TableField("maintenance_result")
    private String maintenanceResult;

    @Schema(description = "维修费用")
    @TableField("maintenance_cost")
    private BigDecimal maintenanceCost;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
