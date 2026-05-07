package com.yx.lab.modules.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yx.lab.modules.asset.entity.LabDocumentShare;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface LabDocumentShareMapper extends BaseMapper<LabDocumentShare> {

    @Delete("DELETE FROM lab_document_share WHERE document_id = #{documentId}")
    int deleteAllByDocumentId(@Param("documentId") Long documentId);
}
