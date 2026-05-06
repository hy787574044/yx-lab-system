package com.yx.lab.modules.asset.dto;

import com.yx.lab.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentQuery extends PageQuery {

    private String keyword;

    private String documentCategory;
}
