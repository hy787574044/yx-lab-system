package com.yx.lab.modules.sample.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.sample.dto.MonitoringPointQuery;
import com.yx.lab.modules.sample.dto.MonitoringPointSaveCommand;
import com.yx.lab.modules.sample.entity.MonitoringPoint;
import com.yx.lab.modules.sample.mapper.MonitoringPointMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 监测点位管理服务，负责点位档案的查询、新增、编辑与删除。
 */
@Service
@RequiredArgsConstructor
public class MonitoringPointService {

    private final MonitoringPointMapper monitoringPointMapper;

    /**
     * 分页查询监测点位列表。
     *
     * @param query 查询条件
     * @return 点位分页结果
     */
    public PageResult<MonitoringPoint> page(MonitoringPointQuery query) {
        Page<MonitoringPoint> page = monitoringPointMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<MonitoringPoint>()
                        .like(StrUtil.isNotBlank(query.getKeyword()), MonitoringPoint::getPointName, query.getKeyword())
                        .eq(StrUtil.isNotBlank(query.getPointType()), MonitoringPoint::getPointType, query.getPointType())
                        .eq(StrUtil.isNotBlank(query.getRegionName()), MonitoringPoint::getRegionName, query.getRegionName())
                        .eq(StrUtil.isNotBlank(query.getPointStatus()), MonitoringPoint::getPointStatus, query.getPointStatus())
                        .orderByDesc(MonitoringPoint::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    /**
     * 获取监测点位详情。
     *
     * @param id 点位ID
     * @return 点位详情
     */
    public MonitoringPoint detail(Long id) {
        return requirePoint(id);
    }

    /**
     * 新增监测点位。
     *
     * @param command 点位保存参数
     */
    public void save(MonitoringPointSaveCommand command) {
        MonitoringPoint point = new MonitoringPoint();
        applyCommand(point, command);
        monitoringPointMapper.insert(point);
    }

    /**
     * 更新监测点位资料。
     *
     * @param id 点位ID
     * @param command 点位保存参数
     */
    public void update(Long id, MonitoringPointSaveCommand command) {
        MonitoringPoint point = requirePoint(id);
        applyCommand(point, command);
        monitoringPointMapper.updateById(point);
    }

    /**
     * 删除监测点位。
     *
     * @param id 点位ID
     */
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
        point.setAddress(StrUtil.trim(command.getAddress()));
        point.setLongitude(StrUtil.trim(command.getLongitude()));
        point.setLatitude(StrUtil.trim(command.getLatitude()));
        point.setRegionName(StrUtil.trim(command.getRegionName()));
        point.setPointType(StrUtil.trim(command.getPointType()));
        point.setPointStatus(StrUtil.trim(command.getPointStatus()));
        validatePoint(point);
    }

    private void validatePoint(MonitoringPoint point) {
        if (StrUtil.isBlank(point.getRegionName())) {
            throw new BusinessException("所属水厂不能为空");
        }
        if (StrUtil.isBlank(point.getPointType())) {
            throw new BusinessException("点位类型不能为空");
        }
        if (!LabWorkflowConstants.POINT_TYPES.contains(point.getPointType())) {
            throw new BusinessException("点位类型不合法");
        }
        ensureUniqueRegionPointType(point);
        if (!LabWorkflowConstants.PointStatus.ENABLED.equals(point.getPointStatus())) {
            return;
        }
        if (StrUtil.isBlank(point.getLatitude()) || StrUtil.isBlank(point.getLongitude())) {
            throw new BusinessException("启用的监测点位必须选择地图坐标");
        }
        validateCoordinate(point.getLatitude(), "纬度", -90D, 90D);
        validateCoordinate(point.getLongitude(), "经度", -180D, 180D);
    }

    private void validateCoordinate(String value, String label, double min, double max) {
        String text = StrUtil.trim(value);
        try {
            double coordinate = Double.parseDouble(text);
            if (coordinate < min || coordinate > max) {
                throw new BusinessException("监测点位" + label + "超出有效范围");
            }
        } catch (NumberFormatException ex) {
            throw new BusinessException("监测点位" + label + "格式不正确");
        }
    }

    private void ensureUniqueRegionPointType(MonitoringPoint point) {
        Long count = monitoringPointMapper.selectCount(new LambdaQueryWrapper<MonitoringPoint>()
                .eq(MonitoringPoint::getRegionName, point.getRegionName())
                .eq(MonitoringPoint::getPointType, point.getPointType())
                .ne(point.getId() != null, MonitoringPoint::getId, point.getId()));
        if (count != null && count > 0) {
            throw new BusinessException("同一所属水厂下已存在该点位类型的监测点位");
        }
    }
}
