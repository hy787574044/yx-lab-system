package com.yx.lab.common.model;

import lombok.Data;

@Data
public class PageQuery {

    private long pageNum = 1L;

    private long pageSize = 10L;
}
