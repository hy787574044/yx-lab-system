package com.yx.lab.common.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.model.PageQuery;

public final class PageUtils {

    private PageUtils() {
    }

    public static <T> Page<T> buildPage(PageQuery pageQuery) {
        return new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
    }
}
