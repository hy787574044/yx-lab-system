package com.yx.lab.modules.asset.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InstrumentMaintenanceSaveCommand {

    @NotNull(message = "设备ID不能为空")
    private Long instrumentId;

    @NotBlank(message = "设备名称不能为空")
    private String instrumentName;

    @NotNull(message = "维护时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime maintenanceTime;

    @NotBlank(message = "维护原因不能为空")
    private String maintenanceReason;

    private String maintainerName;

    private String maintenanceCompany;

    private String maintenanceResult;

    private BigDecimal maintenanceCost;

    private String remark;
}
