package com.yx.lab.modules.unified.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "lab.unified")
public class UnifiedPlatformProperties {

    private String baseUrl;

    private String apiToken;

    private String clientId;

    private String clientSecret;

    private String scope;

    private String responseType = "code";

    private int timeout = 10000;

    private Map<String, String> headers = new LinkedHashMap<>();
}
