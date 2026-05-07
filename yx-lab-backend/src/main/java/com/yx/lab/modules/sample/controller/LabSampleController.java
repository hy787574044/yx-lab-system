package com.yx.lab.modules.sample.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.modules.sample.dto.LabSampleQuery;
import com.yx.lab.modules.sample.dto.SampleLoginCommand;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.service.LabSampleService;
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

/**
 * 样品控制器。
 * 负责样品分页查询、详情查询和样品登录。
 */
@RestController
@RequestMapping("/api/samples")
@RequiredArgsConstructor
@Tag(name = "样品管理")
public class LabSampleController {

    private final LabSampleService labSampleService;

    /**
     * 分页查询样品。
     *
     * @param query 样品查询条件。
     * @return 样品分页结果。
     */
    @GetMapping
    @Operation(summary = "样品分页")
    public ApiResponse<PageResult<LabSample>> page(@Validated LabSampleQuery query) {
        return ApiResponse.success(labSampleService.page(query));
    }

    /**
     * 获取样品详情。
     *
     * @param id 样品主键。
     * @return 样品详情。
     */
    @GetMapping("/{id}")
    @Operation(summary = "样品详情")
    public ApiResponse<LabSample> detail(@PathVariable Long id) {
        return ApiResponse.success(labSampleService.detail(id));
    }

    /**
     * 提交样品登录。
     *
     * @param command 样品登录命令。
     * @return 登录后的样品信息。
     */
    @PostMapping("/login")
    @Operation(summary = "样品登录")
    public ApiResponse<LabSample> login(@Valid @RequestBody SampleLoginCommand command) {
        return ApiResponse.success("样品登录成功", labSampleService.loginSample(command));
    }
}
