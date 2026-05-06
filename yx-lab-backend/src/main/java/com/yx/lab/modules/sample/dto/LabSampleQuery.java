package com.yx.lab.modules.sample.dto;

import com.yx.lab.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LabSampleQuery extends PageQuery {

    private String keyword;

    private String sampleStatus;

    private String sampleType;
}
