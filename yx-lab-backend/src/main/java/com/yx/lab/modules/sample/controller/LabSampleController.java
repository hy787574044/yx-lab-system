package com.yx.lab.modules.sample.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.modules.sample.dto.LabSampleQuery;
import com.yx.lab.modules.sample.dto.SampleLoginCommand;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.service.LabSampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/samples")
@RequiredArgsConstructor
public class LabSampleController {

    private final LabSampleService labSampleService;

    @GetMapping
    public ApiResponse<PageResult<LabSample>> page(@Validated LabSampleQuery query) {
        return ApiResponse.success(labSampleService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<LabSample> detail(@PathVariable Long id) {
        return ApiResponse.success(labSampleService.detail(id));
    }

    @PostMapping("/login")
    public ApiResponse<LabSample> login(@Valid @RequestBody SampleLoginCommand command) {
        return ApiResponse.success("样品登录成功", labSampleService.loginSample(command));
    }
}
