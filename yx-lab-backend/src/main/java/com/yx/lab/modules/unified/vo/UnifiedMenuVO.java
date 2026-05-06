package com.yx.lab.modules.unified.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UnifiedMenuVO {

    private List<UnifiedMenuItemVO> menus = new ArrayList<>();

    private String rawBody;

    private JsonNode raw;
}
