package com.yx.lab.modules.gis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "lab.gis")
public class ThirdPartyGisProperties {

    private String baseUrl;

    private String appKey;

    private String appSecret;

    private String loginName;

    private int timeout = 10000;

    private long refreshBeforeExpireSeconds = 300;
}
