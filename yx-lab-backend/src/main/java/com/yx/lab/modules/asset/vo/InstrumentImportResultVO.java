package com.yx.lab.modules.asset.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class InstrumentImportResultVO {

    private int totalRows;

    private int successCount;

    private boolean allPassed;

    private List<InstrumentImportErrorVO> errors = new ArrayList<>();
}
