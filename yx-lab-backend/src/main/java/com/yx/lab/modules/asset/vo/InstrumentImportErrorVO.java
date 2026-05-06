package com.yx.lab.modules.asset.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentImportErrorVO {

    private int rowNum;

    private String deviceName;

    private String message;
}
