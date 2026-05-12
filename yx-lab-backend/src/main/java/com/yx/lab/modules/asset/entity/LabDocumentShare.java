package com.yx.lab.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档共享关系实体，记录文档与可查看用户之间的授权关系。
 */
@Data
@TableName("lab_document_share")
@EqualsAndHashCode(callSuper = true)
public class LabDocumentShare extends BaseEntity {

    @Schema(description = "文档ID")
    @TableField("document_id")
    private Long documentId;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "用户名")
    @TableField("username")
    private String username;

    @Schema(description = "姓名")
    @TableField("real_name")
    private String realName;
}
