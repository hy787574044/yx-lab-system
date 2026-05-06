package com.yx.lab.modules.unified.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class UnifiedUserInfoVO {

    private String userId;

    private String openId;

    private String username;

    private String realName;

    private String jobNo;

    private String mobile;

    private String departmentName;

    private String rawBody;

    private JsonNode raw;
}
