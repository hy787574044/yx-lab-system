package com.yx.lab.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限声明。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 需要具备的权限码。
     */
    String[] value();

    /**
     * true 表示满足任意一个权限即可，false 表示需要全部满足。
     */
    boolean any() default false;
}
