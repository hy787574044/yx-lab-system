package com.yx.lab.modules.sample.controller;

import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.PermissionConstants;
import com.yx.lab.common.security.RequirePermission;
import com.yx.lab.common.util.ExcelExportUtil;
import com.yx.lab.modules.sample.dto.SamplingTaskActionCommand;
import com.yx.lab.modules.sample.dto.SamplingTaskCompleteCommand;
import com.yx.lab.modules.sample.dto.SamplingTaskQuery;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.service.SamplingTaskService;
import com.yx.lab.modules.sample.vo.StatusCountVO;
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
import java.util.List;

/**
 * 采样任务控制器。
 */
@RestController
@RequestMapping("/api/samplingTasks")
@RequiredArgsConstructor
@Tag(name = "采样任务管理")
@RequirePermission(PermissionConstants.SAMPLING_TASK_VIEW)
public class SamplingTaskController {

    private final SamplingTaskService samplingTaskService;

    @GetMapping
    @Operation(summary = "采样任务分页")
    public ApiResponse<PageResult<SamplingTask>> page(@Validated SamplingTaskQuery query) {
        return ApiResponse.success(samplingTaskService.page(query));
    }

    @GetMapping("/stats")
    @Operation(summary = "采样任务状态统计")
    public ApiResponse<List<StatusCountVO>> stats(@Validated SamplingTaskQuery query) {
        return ApiResponse.success(samplingTaskService.statusStats(query));
    }

    @GetMapping("/export")
    @Operation(summary = "导出采样任务")
    public ResponseEntity<byte[]> export(@Validated SamplingTaskQuery query) {
        ExcelExportUtil.prepareExportQuery(query);
        return ExcelExportUtil.buildResponse(
                "采样任务.xlsx",
                "采样任务",
                samplingTaskService.page(query).getRecords(),
                java.util.Arrays.asList(
                        ExcelExportUtil.column("任务编号", SamplingTask::getTaskNo),
                        ExcelExportUtil.column("样品编号", SamplingTask::getSampleNo),
                        ExcelExportUtil.column("点位名称", SamplingTask::getPointName),
                        ExcelExportUtil.column("采样人员", SamplingTask::getSamplerName),
                        ExcelExportUtil.column("样品类型", item -> LabWorkflowConstants.getSampleTypeLabel(item.getSampleType())),
                        ExcelExportUtil.column("任务状态", item -> LabWorkflowConstants.getSamplingTaskStatusLabel(item.getTaskStatus())),
                        ExcelExportUtil.column("样品登记状态", item -> LabWorkflowConstants.getSampleRegisterStatusLabel(item.getSampleRegisterStatus())),
                        ExcelExportUtil.column("计划采样时间", SamplingTask::getSamplingTime),
                        ExcelExportUtil.column("开始时间", SamplingTask::getStartedTime),
                        ExcelExportUtil.column("完成时间", SamplingTask::getFinishedTime),
                        ExcelExportUtil.column("废弃原因", SamplingTask::getAbandonReason),
                        ExcelExportUtil.column("备注", SamplingTask::getRemark),
                        ExcelExportUtil.column("更新时间", SamplingTask::getUpdatedTime)
                ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "采样任务详情")
    public ApiResponse<SamplingTask> detail(@PathVariable Long id) {
        return ApiResponse.success(samplingTaskService.detail(id));
    }

    @GetMapping("/todo/mine")
    @Operation(summary = "我的采样待办")
    public ApiResponse<List<SamplingTask>> todoMine() {
        return ApiResponse.success(samplingTaskService.todoMine());
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "开始采样任务")
    @RequirePermission(PermissionConstants.SAMPLING_TASK_WRITE)
    public ApiResponse<Void> start(@PathVariable Long id,
                                   @RequestBody(required = false) SamplingTaskActionCommand command) {
        samplingTaskService.start(id, command);
        return ApiResponse.successMessage("任务已开始");
    }

    @PostMapping("/{id}/abandon")
    @Operation(summary = "废弃采样任务")
    @RequirePermission(PermissionConstants.SAMPLING_TASK_WRITE)
    public ApiResponse<Void> abandon(@PathVariable Long id,
                                     @RequestBody(required = false) SamplingTaskActionCommand command) {
        samplingTaskService.abandon(id, command);
        return ApiResponse.successMessage("任务已废弃");
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "恢复采样任务")
    @RequirePermission(PermissionConstants.SAMPLING_TASK_WRITE)
    public ApiResponse<Void> resume(@PathVariable Long id,
                                    @RequestBody(required = false) SamplingTaskActionCommand command) {
        samplingTaskService.resume(id, command);
        return ApiResponse.successMessage("任务已恢复");
    }

    @PostMapping("/complete")
    @Operation(summary = "完成采样任务")
    @RequirePermission(PermissionConstants.SAMPLING_TASK_WRITE)
    public ApiResponse<Void> complete(@Valid @RequestBody SamplingTaskCompleteCommand command) {
        samplingTaskService.complete(command);
        return ApiResponse.successMessage("采样完成");
    }
}
