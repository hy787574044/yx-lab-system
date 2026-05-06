package com.yx.lab.modules.sample.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.modules.sample.dto.SamplingTaskCompleteCommand;
import com.yx.lab.modules.sample.dto.SamplingTaskQuery;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.service.SamplingTaskService;
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
public class SamplingTaskController {

    private final SamplingTaskService samplingTaskService;

    @GetMapping
    public ApiResponse<PageResult<SamplingTask>> page(@Validated SamplingTaskQuery query) {
        return ApiResponse.success(samplingTaskService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<SamplingTask> detail(@PathVariable Long id) {
        return ApiResponse.success(samplingTaskService.detail(id));
    }

    @GetMapping("/todo/mine")
    public ApiResponse<List<SamplingTask>> todoMine() {
        return ApiResponse.success(samplingTaskService.todoMine());
    }

    @PostMapping("/complete")
    public ApiResponse<Void> complete(@Valid @RequestBody SamplingTaskCompleteCommand command) {
        samplingTaskService.complete(command);
        return ApiResponse.successMessage("采样完成");
    }
}
