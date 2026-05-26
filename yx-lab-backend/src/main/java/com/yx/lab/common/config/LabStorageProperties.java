package com.yx.lab.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "lab.storage")
public class LabStorageProperties {

    /**
     * 上传文件存储目录
     */
    private String uploadDir;

    /**
     * 文件服务基础URL（用于拼接全路径）
     */
    private String fileBaseUrl;
}