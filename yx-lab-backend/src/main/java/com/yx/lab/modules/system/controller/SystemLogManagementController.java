package com.yx.lab.modules.system.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.modules.system.dto.LogQuery;
import com.yx.lab.modules.system.service.SystemLogManagementService;
import com.yx.lab.modules.system.vo.SystemLogPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统日志管理控制器。
 */
@RestController
@RequestMapping("/api/system/logs")
@RequiredArgsConstructor
@Tag(name = "系统管理-日志管理")
public class SystemLogManagementController {

    private final SystemLogManagementService systemLogManagementService;

    /**
     * 分页查询系统日志。
     *
     * @param query 查询条件
     * @return 统一日志分页结果
     */
    @GetMapping
    @Operation(summary = "分页查询系统日志")
    public ApiResponse<SystemLogPageVO> page(@Validated LogQuery query) {
        return ApiResponse.success(systemLogManagementService.page(query));
    }
}
