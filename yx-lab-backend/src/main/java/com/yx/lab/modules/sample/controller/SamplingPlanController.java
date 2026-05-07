package com.yx.lab.modules.sample.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.modules.sample.dto.SamplingPlanDispatchCommand;
import com.yx.lab.modules.sample.dto.SamplingPlanQuery;
import com.yx.lab.modules.sample.dto.SamplingPlanSaveCommand;
import com.yx.lab.modules.sample.entity.SamplingPlan;
import com.yx.lab.modules.sample.service.SamplingPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import javax.validation.Valid;

@RestController
@RequestMapping("/api/sampling-plans")
@RequiredArgsConstructor
@Tag(name = "采样计划管理")
public class SamplingPlanController {

    private final SamplingPlanService samplingPlanService;

    @GetMapping
    @Operation(summary = "采样计划分页")
    public ApiResponse<PageResult<SamplingPlan>> page(@Validated SamplingPlanQuery query) {
        return ApiResponse.success(samplingPlanService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "采样计划详情")
    public ApiResponse<SamplingPlan> detail(@PathVariable Long id) {
        return ApiResponse.success(samplingPlanService.detail(id));
    }

    @PostMapping
    @Operation(summary = "新增采样计划")
    public ApiResponse<Void> save(@Valid @RequestBody SamplingPlanSaveCommand command) {
        samplingPlanService.save(command);
        return ApiResponse.successMessage("新增成功");
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新采样计划")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody SamplingPlanSaveCommand command) {
        samplingPlanService.update(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除采样计划")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        samplingPlanService.delete(id);
        return ApiResponse.successMessage("删除成功");
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "暂停采样计划")
    public ApiResponse<Void> pause(@PathVariable Long id) {
        samplingPlanService.pause(id);
        return ApiResponse.successMessage("计划已暂停");
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "恢复采样计划")
    public ApiResponse<Void> resume(@PathVariable Long id) {
        samplingPlanService.resume(id);
        return ApiResponse.successMessage("计划已恢复");
    }

    @PostMapping("/dispatch")
    @Operation(summary = "派发采样计划")
    public ApiResponse<Void> dispatch(@Valid @RequestBody SamplingPlanDispatchCommand command) {
        samplingPlanService.dispatch(command);
        return ApiResponse.successMessage("派发成功");
    }
}
