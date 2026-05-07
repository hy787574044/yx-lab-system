package com.yx.lab.modules.sample.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.modules.sample.dto.SamplingPlanDispatchCommand;
import com.yx.lab.modules.sample.dto.SamplingPlanQuery;
import com.yx.lab.modules.sample.entity.SamplingPlan;
import com.yx.lab.modules.sample.service.SamplingPlanService;
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
public class SamplingPlanController {

    private final SamplingPlanService samplingPlanService;

    @GetMapping
    public ApiResponse<PageResult<SamplingPlan>> page(@Validated SamplingPlanQuery query) {
        return ApiResponse.success(samplingPlanService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<SamplingPlan> detail(@PathVariable Long id) {
        return ApiResponse.success(samplingPlanService.detail(id));
    }

    @PostMapping
    public ApiResponse<Void> save(@RequestBody SamplingPlan plan) {
        samplingPlanService.save(plan);
        return ApiResponse.successMessage("新增成功");
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody SamplingPlan plan) {
        plan.setId(id);
        samplingPlanService.update(plan);
        return ApiResponse.successMessage("更新成功");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        samplingPlanService.delete(id);
        return ApiResponse.successMessage("删除成功");
    }

    @PostMapping("/{id}/pause")
    public ApiResponse<Void> pause(@PathVariable Long id) {
        samplingPlanService.pause(id);
        return ApiResponse.successMessage("计划已暂停");
    }

    @PostMapping("/{id}/resume")
    public ApiResponse<Void> resume(@PathVariable Long id) {
        samplingPlanService.resume(id);
        return ApiResponse.successMessage("计划已恢复");
    }

    @PostMapping("/dispatch")
    public ApiResponse<Void> dispatch(@Valid @RequestBody SamplingPlanDispatchCommand command) {
        samplingPlanService.dispatch(command);
        return ApiResponse.successMessage("派发成功");
    }
}
