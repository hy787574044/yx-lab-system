package com.yx.lab.modules.asset.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class LabDocumentVO {

    private Long id;

    private String documentName;

    private String documentCategory;

    private String fileType;

    private Long fileSize;

    private String fileUrl;

    private String remark;

    private String createdName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    private Boolean canManage;

    @Builder.Default
    private List<Long> viewerUserIds = new ArrayList<>();

    @Builder.Default
    private List<String> viewerNames = new ArrayList<>();
}
