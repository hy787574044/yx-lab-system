package com.yx.lab.modules.asset.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.asset.dto.InstrumentMaintenanceSaveCommand;
import com.yx.lab.modules.asset.dto.InstrumentQuery;
import com.yx.lab.modules.asset.dto.InstrumentSaveCommand;
import com.yx.lab.modules.asset.dto.MaintenanceQuery;
import com.yx.lab.modules.asset.entity.Instrument;
import com.yx.lab.modules.asset.entity.InstrumentMaintenance;
import com.yx.lab.modules.asset.mapper.InstrumentMaintenanceMapper;
import com.yx.lab.modules.asset.mapper.InstrumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 设备台账服务，统一处理设备档案与维修记录的维护。
 */
@Service
@RequiredArgsConstructor
public class InstrumentAssetService {

    private final InstrumentMapper instrumentMapper;

    private final InstrumentMaintenanceMapper maintenanceMapper;

    /**
     * 分页查询设备台账列表。
     *
     * @param query 查询条件
     * @return 设备分页结果
     */
    public PageResult<Instrument> instrumentPage(InstrumentQuery query) {
        Page<Instrument> page = instrumentMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<Instrument>()
                        .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                                .like(Instrument::getInstrumentName, query.getKeyword())
                                .or()
                                .like(Instrument::getInstrumentModel, query.getKeyword())
                                .or()
                                .like(Instrument::getManufacturer, query.getKeyword())
                                .or()
                                .like(Instrument::getStorageLocation, query.getKeyword()))
                        .eq(StrUtil.isNotBlank(query.getInstrumentStatus()), Instrument::getInstrumentStatus, query.getInstrumentStatus())
                        .orderByDesc(Instrument::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    /**
     * 获取设备详情。
     *
     * @param id 设备ID
     * @return 设备详情
     */
    public Instrument instrumentDetail(Long id) {
        return requireInstrument(id);
    }

    /**
     * 新增设备台账。
     *
     * @param command 设备保存参数
     */
    public void saveInstrument(InstrumentSaveCommand command) {
        Instrument entity = new Instrument();
        applyInstrumentCommand(entity, command);
        if (StrUtil.isBlank(entity.getInstrumentStatus())) {
            entity.setInstrumentStatus(LabWorkflowConstants.InstrumentStatus.NORMAL);
        }
        instrumentMapper.insert(entity);
    }

    /**
     * 更新设备台账。
     *
     * @param id 设备ID
     * @param command 设备保存参数
     */
    public void updateInstrument(Long id, InstrumentSaveCommand command) {
        Instrument entity = requireInstrument(id);
        applyInstrumentCommand(entity, command);
        instrumentMapper.updateById(entity);
    }

    /**
     * 删除设备台账。
     *
     * @param id 设备ID
     */
    public void deleteInstrument(Long id) {
        instrumentMapper.deleteById(requireInstrument(id).getId());
    }

    /**
     * 分页查询设备维修记录。
     *
     * @param query 查询条件
     * @return 维修记录分页结果
     */
    public PageResult<InstrumentMaintenance> maintenancePage(MaintenanceQuery query) {
        Page<InstrumentMaintenance> page = maintenanceMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<InstrumentMaintenance>()
                        .eq(query.getInstrumentId() != null, InstrumentMaintenance::getInstrumentId, query.getInstrumentId())
                        .orderByDesc(InstrumentMaintenance::getMaintenanceTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    /**
     * 新增设备维修记录。
     *
     * @param command 维修保存参数
     */
    public void saveMaintenance(InstrumentMaintenanceSaveCommand command) {
        InstrumentMaintenance entity = new InstrumentMaintenance();
        applyMaintenanceCommand(entity, command);
        maintenanceMapper.insert(entity);
    }

    /**
     * 更新设备维修记录。
     *
     * @param id 维修记录ID
     * @param command 维修保存参数
     */
    public void updateMaintenance(Long id, InstrumentMaintenanceSaveCommand command) {
        InstrumentMaintenance entity = requireMaintenance(id);
        applyMaintenanceCommand(entity, command);
        maintenanceMapper.updateById(entity);
    }

    /**
     * 删除设备维修记录。
     *
     * @param id 维修记录ID
     */
    public void deleteMaintenance(Long id) {
        maintenanceMapper.deleteById(requireMaintenance(id).getId());
    }

    private Instrument requireInstrument(Long id) {
        Instrument instrument = instrumentMapper.selectById(id);
        if (instrument == null) {
            throw new BusinessException("Instrument not found");
        }
        return instrument;
    }

    private InstrumentMaintenance requireMaintenance(Long id) {
        InstrumentMaintenance maintenance = maintenanceMapper.selectById(id);
        if (maintenance == null) {
            throw new BusinessException("Maintenance record not found");
        }
        return maintenance;
    }

    private void applyInstrumentCommand(Instrument entity, InstrumentSaveCommand command) {
        entity.setInstrumentName(StrUtil.trim(command.getInstrumentName()));
        entity.setInstrumentModel(StrUtil.trim(command.getInstrumentModel()));
        entity.setManufacturer(StrUtil.trim(command.getManufacturer()));
        entity.setPurchaseDate(command.getPurchaseDate());
        entity.setServiceLifeYears(command.getServiceLifeYears());
        entity.setCalibrationCycle(StrUtil.trim(command.getCalibrationCycle()));
        entity.setOwnerName(StrUtil.trim(command.getOwnerName()));
        entity.setInstrumentStatus(StrUtil.trim(command.getInstrumentStatus()));
        entity.setStorageLocation(StrUtil.trim(command.getStorageLocation()));
        entity.setCertificateUrl(StrUtil.trim(command.getCertificateUrl()));
        entity.setRemark(StrUtil.trim(command.getRemark()));
    }

    private void applyMaintenanceCommand(InstrumentMaintenance entity, InstrumentMaintenanceSaveCommand command) {
        requireInstrument(command.getInstrumentId());
        entity.setInstrumentId(command.getInstrumentId());
        entity.setInstrumentName(StrUtil.trim(command.getInstrumentName()));
        entity.setMaintenanceTime(command.getMaintenanceTime());
        entity.setMaintenanceReason(StrUtil.trim(command.getMaintenanceReason()));
        entity.setMaintainerName(StrUtil.trim(command.getMaintainerName()));
        entity.setMaintenanceCompany(StrUtil.trim(command.getMaintenanceCompany()));
        entity.setMaintenanceResult(StrUtil.trim(command.getMaintenanceResult()));
        entity.setMaintenanceCost(command.getMaintenanceCost());
        entity.setRemark(StrUtil.trim(command.getRemark()));
    }
}
