package com.yx.lab.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档台账实体，记录制度文件、操作规程等资料的元数据。
 */
@Data
@TableName("lab_document")
@EqualsAndHashCode(callSuper = true)
public class LabDocument extends BaseEntity {

    @Schema(description = "文档名称")
    @TableField("document_name")
    private String documentName;

    @Schema(description = "文档分类")
    @TableField("document_category")
    private String documentCategory;

    @Schema(description = "文件类型")
    @TableField("file_type")
    private String fileType;

    @Schema(description = "文件大小，单位字节")
    @TableField("file_size")
    private Long fileSize;

    @Schema(description = "文件地址")
    @TableField("file_url")
    private String fileUrl;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
