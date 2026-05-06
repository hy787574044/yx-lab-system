package com.yx.lab.common.config;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                CurrentUser currentUser = SecurityContext.getCurrentUser();
                strictInsertFill(metaObject, "createdTime", LocalDateTime.class, LocalDateTime.now());
                strictInsertFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
                strictInsertFill(metaObject, "deleted", Integer.class, 0);
                if (currentUser != null) {
                    strictInsertFill(metaObject, "createdBy", Long.class, currentUser.getUserId());
                    strictInsertFill(metaObject, "createdName", String.class, currentUser.getRealName());
                    strictInsertFill(metaObject, "updatedBy", Long.class, currentUser.getUserId());
                    strictInsertFill(metaObject, "updatedName", String.class, currentUser.getRealName());
                }
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                CurrentUser currentUser = SecurityContext.getCurrentUser();
                strictUpdateFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
                if (currentUser != null) {
                    strictUpdateFill(metaObject, "updatedBy", Long.class, currentUser.getUserId());
                    strictUpdateFill(metaObject, "updatedName", String.class, currentUser.getRealName());
                }
            }
        };
    }
}
