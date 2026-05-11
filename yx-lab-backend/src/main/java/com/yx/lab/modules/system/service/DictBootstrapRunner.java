package com.yx.lab.modules.system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 系统启动时补齐内置数据字典。
 */
@Component
@RequiredArgsConstructor
public class DictBootstrapRunner implements ApplicationRunner {

    private final DictManagementService dictManagementService;

    @Override
    public void run(ApplicationArguments args) {
        dictManagementService.ensureBuiltInDicts();
    }
}
