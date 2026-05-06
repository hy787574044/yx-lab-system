package com.yx.lab.modules.sample.dto;

import com.yx.lab.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MonitoringPointQuery extends PageQuery {

    private String keyword;

    private String pointType;

    private String pointStatus;
}
