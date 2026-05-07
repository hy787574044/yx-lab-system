package com.yx.lab.modules.asset.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

@Data
public class InstrumentSaveCommand {

    @NotBlank(message = "设备名称不能为空")
    private String instrumentName;

    private String instrumentModel;

    private String manufacturer;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    private Integer serviceLifeYears;

    private String calibrationCycle;

    private String ownerName;

    @NotBlank(message = "设备状态不能为空")
    private String instrumentStatus;

    private String storageLocation;

    private String certificateUrl;

    private String remark;
}
