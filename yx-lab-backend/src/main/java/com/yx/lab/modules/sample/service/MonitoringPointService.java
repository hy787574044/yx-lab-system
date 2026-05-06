package com.yx.lab.modules.sample.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.sample.dto.MonitoringPointQuery;
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
        return monitoringPointMapper.selectById(id);
    }

    public void save(MonitoringPoint point) {
        monitoringPointMapper.insert(point);
    }

    public void update(MonitoringPoint point) {
        monitoringPointMapper.updateById(point);
    }

    public void delete(Long id) {
        monitoringPointMapper.deleteById(id);
    }
}
