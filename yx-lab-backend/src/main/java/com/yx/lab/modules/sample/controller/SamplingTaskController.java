package com.yx.lab.modules.sample.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.modules.sample.dto.SamplingTaskActionCommand;
import com.yx.lab.modules.sample.dto.SamplingTaskCompleteCommand;
import com.yx.lab.modules.sample.dto.SamplingTaskQuery;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.service.SamplingTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/sampling-tasks")
@RequiredArgsConstructor
@Tag(name = "采样任务管理")
public class SamplingTaskController {

    private final SamplingTaskService samplingTaskService;

    @GetMapping
    @Operation(summary = "采样任务分页")
    public ApiResponse<PageResult<SamplingTask>> page(@Validated SamplingTaskQuery query) {
        return ApiResponse.success(samplingTaskService.page(query));
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
    public ApiResponse<Void> start(@PathVariable Long id,
                                   @RequestBody(required = false) SamplingTaskActionCommand command) {
        samplingTaskService.start(id, command);
        return ApiResponse.successMessage("任务已开始");
    }

    @PostMapping("/{id}/abandon")
    @Operation(summary = "废弃采样任务")
    public ApiResponse<Void> abandon(@PathVariable Long id,
                                     @RequestBody(required = false) SamplingTaskActionCommand command) {
        samplingTaskService.abandon(id, command);
        return ApiResponse.successMessage("任务已废弃");
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "恢复采样任务")
    public ApiResponse<Void> resume(@PathVariable Long id,
                                    @RequestBody(required = false) SamplingTaskActionCommand command) {
        samplingTaskService.resume(id, command);
        return ApiResponse.successMessage("任务已恢复");
    }

    @PostMapping("/complete")
    @Operation(summary = "完成采样任务")
    public ApiResponse<Void> complete(@Valid @RequestBody SamplingTaskCompleteCommand command) {
        samplingTaskService.complete(command);
        return ApiResponse.successMessage("采样完成");
    }
}
