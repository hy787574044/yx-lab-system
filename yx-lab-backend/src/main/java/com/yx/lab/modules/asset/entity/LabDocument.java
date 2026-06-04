package com.yx.lab.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("lab_document")
@EqualsAndHashCode(callSuper = true)
public class LabDocument extends BaseEntity {

    private String documentName;

    private String documentCategory;

    private String fileType;

    private Long fileSize;

    private String fileUrl;

    private String remark;
}
