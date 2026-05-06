package com.yx.lab.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "lab.storage")
public class LabStorageProperties {

    private String uploadDir;
}
