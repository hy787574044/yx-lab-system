package com.yx.lab.modules.sample.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.util.ExcelExportUtil;
import com.yx.lab.modules.sample.dto.SamplingPlanDispatchCommand;
import com.yx.lab.modules.sample.dto.SamplingPlanQuery;
import com.yx.lab.modules.sample.dto.SamplingPlanSaveCommand;
import com.yx.lab.modules.sample.entity.SamplingPlan;
import com.yx.lab.modules.sample.service.SamplingPlanService;
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

/**
 * 采样计划控制器。
 * 负责采样计划的维护、暂停恢复与派发。
 */
@RestController
@RequestMapping("/api/samplingPlans")
@RequiredArgsConstructor
@Tag(name = "采样计划管理")
public class SamplingPlanController {

    private final SamplingPlanService samplingPlanService;

    /**
     * 分页查询采样计划。
     *
     * @param query 采样计划查询条件。
     * @return 采样计划分页结果。
     */
    @GetMapping
    @Operation(summary = "采样计划分页")
    public ApiResponse<PageResult<SamplingPlan>> page(@Validated SamplingPlanQuery query) {
        return ApiResponse.success(samplingPlanService.page(query));
    }

    /**
     * 导出采样计划。
     *
     * @param query 采样计划查询条件。
     * @return Excel 文件流。
     */
    @GetMapping("/export")
    @Operation(summary = "导出采样计划")
    public ResponseEntity<byte[]> export(@Validated SamplingPlanQuery query) {
        ExcelExportUtil.prepareExportQuery(query);
        return ExcelExportUtil.buildResponse(
                "采样计划.xlsx",
                "采样计划",
                samplingPlanService.page(query).getRecords(),
                java.util.Arrays.asList(
                        ExcelExportUtil.column("计划名称", SamplingPlan::getPlanName),
                        ExcelExportUtil.column("采样点位", SamplingPlan::getPointName),
                        ExcelExportUtil.column("采样人员", SamplingPlan::getSamplerName),
                        ExcelExportUtil.column("周期类型", item -> LabWorkflowConstants.getCycleTypeLabel(item.getCycleType())),
                        ExcelExportUtil.column("样品类型", item -> LabWorkflowConstants.getSampleTypeLabel(item.getSampleType())),
                        ExcelExportUtil.column("开始时间", SamplingPlan::getStartTime),
                        ExcelExportUtil.column("结束时间", SamplingPlan::getEndTime),
                        ExcelExportUtil.column("计划状态", item -> LabWorkflowConstants.getSamplingPlanStatusLabel(item.getPlanStatus())),
                        ExcelExportUtil.column("备注", SamplingPlan::getRemark),
                        ExcelExportUtil.column("更新时间", SamplingPlan::getUpdatedTime)
                ));
    }

    /**
     * 获取采样计划详情。
     *
     * @param id 采样计划主键。
     * @return 采样计划详情。
     */
    @GetMapping("/{id}")
    @Operation(summary = "采样计划详情")
    public ApiResponse<SamplingPlan> detail(@PathVariable Long id) {
        return ApiResponse.success(samplingPlanService.detail(id));
    }

    /**
     * 新增采样计划。
     *
     * @param command 采样计划保存命令。
     * @return 保存结果。
     */
    @PostMapping
    @Operation(summary = "新增采样计划")
    public ApiResponse<Void> save(@Valid @RequestBody SamplingPlanSaveCommand command) {
        samplingPlanService.save(command);
        return ApiResponse.successMessage("新增成功");
    }

    /**
     * 更新采样计划。
     *
     * @param id 采样计划主键。
     * @param command 采样计划保存命令。
     * @return 更新结果。
     */
    @PostMapping("/{id}")
    @Operation(summary = "更新采样计划")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody SamplingPlanSaveCommand command) {
        samplingPlanService.update(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    /**
     * 删除采样计划。
     *
     * @param id 采样计划主键。
     * @return 删除结果。
     */
    @PostMapping("/{id}/delete")
    @Operation(summary = "删除采样计划")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        samplingPlanService.delete(id);
        return ApiResponse.successMessage("删除成功");
    }

    /**
     * 暂停采样计划。
     *
     * @param id 采样计划主键。
     * @return 暂停结果。
     */
    @PostMapping("/{id}/pause")
    @Operation(summary = "暂停采样计划")
    public ApiResponse<Void> pause(@PathVariable Long id) {
        samplingPlanService.pause(id);
        return ApiResponse.successMessage("计划已暂停");
    }

    /**
     * 恢复采样计划。
     *
     * @param id 采样计划主键。
     * @return 恢复结果。
     */
    @PostMapping("/{id}/resume")
    @Operation(summary = "恢复采样计划")
    public ApiResponse<Void> resume(@PathVariable Long id) {
        samplingPlanService.resume(id);
        return ApiResponse.successMessage("计划已恢复");
    }

    /**
     * 派发采样计划。
     *
     * @param command 采样计划派发命令。
     * @return 派发结果。
     */
    @PostMapping("/dispatch")
    @Operation(summary = "派发采样计划")
    public ApiResponse<Void> dispatch(@Valid @RequestBody SamplingPlanDispatchCommand command) {
        samplingPlanService.dispatch(command);
        return ApiResponse.successMessage("派发成功");
    }
}
