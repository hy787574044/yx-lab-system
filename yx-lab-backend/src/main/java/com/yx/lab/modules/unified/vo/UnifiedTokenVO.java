package com.yx.lab.modules.unified.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class UnifiedTokenVO {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private Long expiresIn;

    private String scope;

    private String openId;

    private String rawBody;

    private JsonNode raw;
}
