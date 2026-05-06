package com.yx.lab.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

@Data
@TableName("lab_instrument")
@EqualsAndHashCode(callSuper = true)
public class Instrument extends BaseEntity {

    @NotBlank(message = "设备名称不能为空")
    private String instrumentName;

    private String instrumentModel;

    private String manufacturer;

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
