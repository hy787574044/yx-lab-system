package com.yx.lab.modules.asset.dto;

import com.yx.lab.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InstrumentQuery extends PageQuery {

    private String keyword;

    private String instrumentStatus;
}
