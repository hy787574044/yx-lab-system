package com.yx.lab.modules.detection.dto;

import com.yx.lab.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DetectionRecordQuery extends PageQuery {

    private String keyword;

    private String detectionStatus;

    private Boolean mine;
}
