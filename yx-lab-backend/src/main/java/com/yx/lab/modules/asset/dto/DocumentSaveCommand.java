package com.yx.lab.modules.asset.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Data
public class DocumentSaveCommand {

    @NotBlank(message = "文档名称不能为空")
    private String documentName;

    private String documentCategory;

    private String fileType;

    private Long fileSize;

    @NotBlank(message = "请先上传文档文件")
    private String fileUrl;

    private String remark;

    private List<String> viewerUserIds = new ArrayList<>();
}
