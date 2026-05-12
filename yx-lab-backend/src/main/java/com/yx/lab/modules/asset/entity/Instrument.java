package com.yx.lab.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * 设备台账实体，记录化验室设备的基础档案信息。
 */
@Data
@TableName("lab_instrument")
@EqualsAndHashCode(callSuper = true)
public class Instrument extends BaseEntity {

    @NotBlank(message = "设备名称不能为空")
    @Schema(description = "设备名称")
    @TableField("instrument_name")
    private String instrumentName;

    @Schema(description = "设备型号")
    @TableField("instrument_model")
    private String instrumentModel;

    @Schema(description = "生产厂家")
    @TableField("manufacturer")
    private String manufacturer;

    @Schema(description = "购置日期")
    @TableField("purchase_date")
    private LocalDate purchaseDate;

    @Schema(description = "使用年限")
    @TableField("service_life_years")
    private Integer serviceLifeYears;

    @Schema(description = "校准周期")
    @TableField("calibration_cycle")
    private String calibrationCycle;

    @Schema(description = "负责人")
    @TableField("owner_name")
    private String ownerName;

    @NotBlank(message = "设备状态不能为空")
    @Schema(description = "设备状态")
    @TableField("instrument_status")
    private String instrumentStatus;

    @Schema(description = "存放位置")
    @TableField("storage_location")
    private String storageLocation;

    @Schema(description = "证书地址")
    @TableField("certificate_url")
    private String certificateUrl;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
