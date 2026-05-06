package com.yx.lab.modules.asset.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.asset.dto.DocumentQuery;
import com.yx.lab.modules.asset.dto.InstrumentQuery;
import com.yx.lab.modules.asset.dto.MaintenanceQuery;
import com.yx.lab.modules.asset.entity.Instrument;
import com.yx.lab.modules.asset.entity.InstrumentMaintenance;
import com.yx.lab.modules.asset.entity.LabDocument;
import com.yx.lab.modules.asset.mapper.InstrumentMaintenanceMapper;
import com.yx.lab.modules.asset.mapper.InstrumentMapper;
import com.yx.lab.modules.asset.mapper.LabDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final InstrumentMapper instrumentMapper;

    private final InstrumentMaintenanceMapper maintenanceMapper;

    private final LabDocumentMapper documentMapper;

    public PageResult<Instrument> instrumentPage(InstrumentQuery query) {
        Page<Instrument> page = instrumentMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<Instrument>()
                        .like(StrUtil.isNotBlank(query.getKeyword()), Instrument::getInstrumentName, query.getKeyword())
                        .eq(StrUtil.isNotBlank(query.getInstrumentStatus()), Instrument::getInstrumentStatus, query.getInstrumentStatus())
                        .orderByDesc(Instrument::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public PageResult<InstrumentMaintenance> maintenancePage(MaintenanceQuery query) {
        Page<InstrumentMaintenance> page = maintenanceMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<InstrumentMaintenance>()
                        .eq(query.getInstrumentId() != null, InstrumentMaintenance::getInstrumentId, query.getInstrumentId())
                        .orderByDesc(InstrumentMaintenance::getMaintenanceTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public PageResult<LabDocument> documentPage(DocumentQuery query) {
        Page<LabDocument> page = documentMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<LabDocument>()
                        .like(StrUtil.isNotBlank(query.getKeyword()), LabDocument::getDocumentName, query.getKeyword())
                        .eq(StrUtil.isNotBlank(query.getDocumentCategory()), LabDocument::getDocumentCategory, query.getDocumentCategory())
                        .orderByDesc(LabDocument::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public void saveInstrument(Instrument entity) {
        instrumentMapper.insert(entity);
    }

    public void updateInstrument(Instrument entity) {
        instrumentMapper.updateById(entity);
    }

    public void deleteInstrument(Long id) {
        instrumentMapper.deleteById(id);
    }

    public void saveMaintenance(InstrumentMaintenance entity) {
        maintenanceMapper.insert(entity);
    }

    public void updateMaintenance(InstrumentMaintenance entity) {
        maintenanceMapper.updateById(entity);
    }

    public void deleteMaintenance(Long id) {
        maintenanceMapper.deleteById(id);
    }

    public void saveDocument(LabDocument entity) {
        documentMapper.insert(entity);
    }

    public void updateDocument(LabDocument entity) {
        documentMapper.updateById(entity);
    }

    public void deleteDocument(Long id) {
        documentMapper.deleteById(id);
    }
}
