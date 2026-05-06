package com.yx.lab.modules.sample.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.modules.sample.dto.MonitoringPointQuery;
import com.yx.lab.modules.sample.entity.MonitoringPoint;
import com.yx.lab.modules.sample.service.MonitoringPointService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoring-points")
@RequiredArgsConstructor
public class MonitoringPointController {

    private final MonitoringPointService monitoringPointService;

    @GetMapping
    @Operation(summary = "监测点位分页")
    public ApiResponse<PageResult<MonitoringPoint>> page(@Validated MonitoringPointQuery query) {
        return ApiResponse.success(monitoringPointService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<MonitoringPoint> detail(@PathVariable Long id) {
        return ApiResponse.success(monitoringPointService.detail(id));
    }

    @PostMapping
    public ApiResponse<Void> save(@RequestBody MonitoringPoint point) {
        monitoringPointService.save(point);
        return ApiResponse.successMessage("新增成功");
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody MonitoringPoint point) {
        point.setId(id);
        monitoringPointService.update(point);
        return ApiResponse.successMessage("更新成功");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        monitoringPointService.delete(id);
        return ApiResponse.successMessage("删除成功");
    }
}
