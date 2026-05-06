package com.yx.lab.modules.unified.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class UnifiedAuthorizeCodeVO {

    private String code;

    private String rawBody;

    private JsonNode raw;
}
