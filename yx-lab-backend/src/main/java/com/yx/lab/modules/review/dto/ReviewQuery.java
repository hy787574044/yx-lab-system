package com.yx.lab.modules.review.dto;

import com.yx.lab.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReviewQuery extends PageQuery {

    private String keyword;

    private String reviewResult;

    private Boolean mine;
}
