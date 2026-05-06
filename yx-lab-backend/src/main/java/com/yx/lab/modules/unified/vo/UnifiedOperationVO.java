package com.yx.lab.modules.unified.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class UnifiedOperationVO {

    private Boolean success;

    private String message;

    private String rawBody;

    private JsonNode raw;
}
