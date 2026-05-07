package com.yx.lab.modules.sample.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.sample.dto.MonitoringPointQuery;
import com.yx.lab.modules.sample.dto.MonitoringPointSaveCommand;
import com.yx.lab.modules.sample.entity.MonitoringPoint;
import com.yx.lab.modules.sample.mapper.MonitoringPointMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MonitoringPointService {

    private final MonitoringPointMapper monitoringPointMapper;

    public PageResult<MonitoringPoint> page(MonitoringPointQuery query) {
        Page<MonitoringPoint> page = monitoringPointMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<MonitoringPoint>()
                        .like(StrUtil.isNotBlank(query.getKeyword()), MonitoringPoint::getPointName, query.getKeyword())
                        .eq(StrUtil.isNotBlank(query.getPointType()), MonitoringPoint::getPointType, query.getPointType())
                        .eq(StrUtil.isNotBlank(query.getPointStatus()), MonitoringPoint::getPointStatus, query.getPointStatus())
                        .orderByDesc(MonitoringPoint::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public MonitoringPoint detail(Long id) {
        return requirePoint(id);
    }

    public void save(MonitoringPointSaveCommand command) {
        MonitoringPoint point = new MonitoringPoint();
        applyCommand(point, command);
        monitoringPointMapper.insert(point);
    }

    public void update(Long id, MonitoringPointSaveCommand command) {
        MonitoringPoint point = requirePoint(id);
        applyCommand(point, command);
        monitoringPointMapper.updateById(point);
    }

    public void delete(Long id) {
        monitoringPointMapper.deleteById(requirePoint(id).getId());
    }

    private MonitoringPoint requirePoint(Long id) {
        MonitoringPoint point = monitoringPointMapper.selectById(id);
        if (point == null) {
            throw new BusinessException("监测点位不存在");
        }
        return point;
    }

    private void applyCommand(MonitoringPoint point, MonitoringPointSaveCommand command) {
        point.setPointName(StrUtil.trim(command.getPointName()));
        point.setLongitude(StrUtil.trim(command.getLongitude()));
        point.setLatitude(StrUtil.trim(command.getLatitude()));
        point.setRegionName(StrUtil.trim(command.getRegionName()));
        point.setServicePopulation(command.getServicePopulation());
        point.setFrequencyType(StrUtil.trim(command.getFrequencyType()));
        point.setOwnerId(command.getOwnerId());
        point.setOwnerName(StrUtil.trim(command.getOwnerName()));
        point.setContactPhone(StrUtil.trim(command.getContactPhone()));
        point.setPointType(StrUtil.trim(command.getPointType()));
        point.setPointStatus(StrUtil.trim(command.getPointStatus()));
    }
}
