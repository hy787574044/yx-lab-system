package com.yx.lab.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("lab_document_share")
@EqualsAndHashCode(callSuper = true)
public class LabDocumentShare extends BaseEntity {

    private Long documentId;

    private Long userId;

    private String username;

    private String realName;
}
