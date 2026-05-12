package com.yx.lab.modules.system.controller;

import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.util.ExcelExportUtil;
import com.yx.lab.modules.system.dto.LogQuery;
import com.yx.lab.modules.system.service.SystemLogManagementService;
import com.yx.lab.modules.system.vo.SystemLogPageVO;
import com.yx.lab.modules.system.vo.SystemLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    /**
     * 导出系统日志。
     *
     * @param query 查询条件
     * @return Excel 文件流
     */
    @GetMapping("/export")
    @Operation(summary = "导出系统日志")
    public ResponseEntity<byte[]> export(@Validated LogQuery query) {
        ExcelExportUtil.prepareExportQuery(query);
        SystemLogPageVO pageVO = systemLogManagementService.page(query);
        return ExcelExportUtil.buildResponse(
                "系统日志.xlsx",
                "系统日志",
                pageVO.getRecords(),
                java.util.Arrays.asList(
                        ExcelExportUtil.column("发生时间", SystemLogVO::getOccurTime),
                        ExcelExportUtil.column("日志来源", SystemLogVO::getSourceName),
                        ExcelExportUtil.column("业务编号", SystemLogVO::getBusinessNo),
                        ExcelExportUtil.column("操作人", SystemLogVO::getOperatorName),
                        ExcelExportUtil.column("状态", item -> LabWorkflowConstants.translateWorkflowText(item.getStatus())),
                        ExcelExportUtil.column("日志内容", item -> LabWorkflowConstants.translateWorkflowText(item.getContent()))
                ));
    }
}
