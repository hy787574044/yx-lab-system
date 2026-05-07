package com.yx.lab.modules.asset.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentPreviewFile {

    private String fileName;

    private String contentType;

    private byte[] content;
}
