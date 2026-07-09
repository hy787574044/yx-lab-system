package com.yx.lab.modules.gis.model;

import lombok.Data;

@Data
public class ThirdPartyGisTokenCache {

    private String accessToken;

    private String tokenType;

    private Long expiresIn;

    private String refreshToken;

    private Long cachedAtMillis;

    private Long expireAtMillis;
}
