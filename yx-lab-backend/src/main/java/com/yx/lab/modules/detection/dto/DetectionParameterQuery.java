package com.yx.lab.modules.detection.dto;

import com.yx.lab.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DetectionParameterQuery extends PageQuery {

    private String keyword;
}
