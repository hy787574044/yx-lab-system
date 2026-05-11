package com.yx.lab.modules.detection.dto;

import com.yx.lab.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检测项目组查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DetectionProjectGroupQuery extends PageQuery {

    private String keyword;

    private Integer enabled;
}
