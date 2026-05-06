package com.yx.lab.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "lab.security")
public class LabSecurityProperties {

    private String tokenHeader;

    private String tokenPrefix;

    private long tokenExpireHours;
}
