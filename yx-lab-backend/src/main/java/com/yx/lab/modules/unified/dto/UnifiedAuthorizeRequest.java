package com.yx.lab.modules.unified.dto;

import lombok.Data;

@Data
public class UnifiedAuthorizeRequest {

    private String responseType;

    private String clientId;

    private String scope;
}
