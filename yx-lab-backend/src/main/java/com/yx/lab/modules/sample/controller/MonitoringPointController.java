package com.yx.lab.modules.sample.controller;

import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.PermissionConstants;
import com.yx.lab.common.security.RequirePermission;
import com.yx.lab.common.util.ExcelExportUtil;
import com.yx.lab.modules.sample.dto.MonitoringPointQuery;
import com.yx.lab.modules.sample.dto.MonitoringPointSaveCommand;
import com.yx.lab.modules.sample.entity.MonitoringPoint;
import com.yx.lab.modules.sample.service.MonitoringPointService;
import com.yx.lab.modules.system.service.OrgManagementService;
import com.yx.lab.modules.system.vo.OrgOptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/monitoringPoints")
@RequiredArgsConstructor
@Tag(name = "监测点位管理")
@RequirePermission(PermissionConstants.SYSTEM_VIEW)
public class MonitoringPointController {

    private final MonitoringPointService monitoringPointService;

    private final OrgManagementService orgManagementService;

    @GetMapping
    @Operation(summary = "监测点位分页")
    public ApiResponse<PageResult<MonitoringPoint>> page(@Validated MonitoringPointQuery query) {
        return ApiResponse.success(monitoringPointService.page(query));
    }

    @GetMapping("/orgOptions")
    @Operation(summary = "获取一级机构选项")
    @RequirePermission(any = true, value = {
            PermissionConstants.SYSTEM_VIEW,
            PermissionConstants.SAMPLE_VIEW,
            PermissionConstants.SAMPLING_PLAN_VIEW,
            PermissionConstants.DETECTION_VIEW,
            PermissionConstants.STATISTICS_VIEW
    })
    public ApiResponse<List<OrgOptionVO>> orgOptions() {
        return ApiResponse.success(orgManagementService.getFirstLevelOrgs());
    }

    @GetMapping("/export")
    @Operation(summary = "导出监测点位")
    public ResponseEntity<byte[]> export(@Validated MonitoringPointQuery query) {
        ExcelExportUtil.prepareExportQuery(query);
        return ExcelExportUtil.buildResponse(
                "监测点位.xlsx",
                "监测点位",
                monitoringPointService.page(query).getRecords(),
                Arrays.asList(
                        ExcelExportUtil.column("点位名称", MonitoringPoint::getPointName),
                        ExcelExportUtil.column("地图位置", MonitoringPoint::getAddress),
                        ExcelExportUtil.column("所属机构", item -> item.getOrgName() != null ? item.getOrgName() : "-"),
                        ExcelExportUtil.column("点位类型", item -> LabWorkflowConstants.getPointTypeLabel(item.getPointType())),
                        ExcelExportUtil.column("经度", MonitoringPoint::getLongitude),
                        ExcelExportUtil.column("纬度", MonitoringPoint::getLatitude),
                        ExcelExportUtil.column("状态", item -> LabWorkflowConstants.getPointStatusLabel(item.getPointStatus())),
                        ExcelExportUtil.column("创建时间", MonitoringPoint::getCreatedTime),
                        ExcelExportUtil.column("更新时间", MonitoringPoint::getUpdatedTime)
                ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "监测点位详情")
    public ApiResponse<MonitoringPoint> detail(@PathVariable("id") Long id) {
        return ApiResponse.success(monitoringPointService.detail(id));
    }

    @PostMapping
    @Operation(summary = "新增监测点位")
    @RequirePermission(PermissionConstants.SYSTEM_WRITE)
    public ApiResponse<Void> save(@Valid @RequestBody MonitoringPointSaveCommand command) {
        monitoringPointService.save(command);
        return ApiResponse.successMessage("新增成功");
    }

    @PostMapping("/{id}")
    @Operation(summary = "更新监测点位")
    @RequirePermission(PermissionConstants.SYSTEM_WRITE)
    public ApiResponse<Void> update(@PathVariable("id") Long id, @Valid @RequestBody MonitoringPointSaveCommand command) {
        monitoringPointService.update(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    @PostMapping("/{id}/delete")
    @Operation(summary = "删除监测点位")
    @RequirePermission(PermissionConstants.SYSTEM_WRITE)
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        monitoringPointService.delete(id);
        return ApiResponse.successMessage("删除成功");
    }
}
