package com.yx.lab.modules.detection.vo;

import lombok.Data;

/**
 * 检测员下拉选项。
 */
@Data
public class DetectionDetectorOptionVO {

    private Long userId;

    private String username;

    private String realName;

    private String roleCode;

    private String displayName;
}
