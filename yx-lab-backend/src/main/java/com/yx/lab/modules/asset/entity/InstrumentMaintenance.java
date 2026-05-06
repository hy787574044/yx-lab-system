package com.yx.lab.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("lab_instrument_maintenance")
@EqualsAndHashCode(callSuper = true)
public class InstrumentMaintenance extends BaseEntity {

    private Long instrumentId;

    private String instrumentName;

    private LocalDateTime maintenanceTime;

    private String maintenanceReason;

    private String maintainerName;

    private String maintenanceCompany;

    private String maintenanceResult;

    private BigDecimal maintenanceCost;

    private String remark;
}
