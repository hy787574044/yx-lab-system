package com.yx.lab.modules.gis.vo;

import lombok.Data;

@Data
public class ThirdPartyGisTokenVO {

    private String accessToken;

    private String tokenType;

    private Long expiresIn;

    private String refreshToken;

    private Long cachedAtMillis;

    private Long expireAtMillis;

    private String cacheSource;
}
