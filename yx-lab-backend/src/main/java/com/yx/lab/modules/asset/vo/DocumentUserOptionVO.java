package com.yx.lab.modules.asset.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentUserOptionVO {

    private Long userId;

    private String username;

    private String realName;

    private String roleCode;
}
