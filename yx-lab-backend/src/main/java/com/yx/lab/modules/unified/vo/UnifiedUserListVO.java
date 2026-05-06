package com.yx.lab.modules.unified.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UnifiedUserListVO {

    private List<UnifiedUserInfoVO> users = new ArrayList<>();

    private String rawBody;

    private JsonNode raw;
}
