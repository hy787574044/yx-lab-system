package com.yx.lab.modules.unified.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UnifiedMenuItemVO {

    private String menuId;

    private String menuName;

    private String menuCode;

    private String path;

    private String component;

    private List<UnifiedMenuItemVO> children = new ArrayList<>();

    private JsonNode raw;
}
