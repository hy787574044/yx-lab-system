package com.yx.lab.modules.report.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.PermissionConstants;
import com.yx.lab.common.security.RequirePermission;
import com.yx.lab.modules.report.dto.SummaryReportPreviewQuery;
import com.yx.lab.modules.report.dto.SummaryReportQuery;
import com.yx.lab.modules.report.service.SummaryReportService;
import com.yx.lab.modules.report.vo.SummaryReportListVO;
import com.yx.lab.modules.report.vo.SummaryReportPreviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportSummaries")
@RequiredArgsConstructor
@Tag(name = "汇总报表管理")
@RequirePermission(PermissionConstants.REPORT_VIEW)
public class SummaryReportController {

    private final SummaryReportService summaryReportService;

    @GetMapping
    @Operation(summary = "汇总报表分页")
    public ApiResponse<PageResult<SummaryReportListVO>> page(@Validated SummaryReportQuery query) {
        return ApiResponse.success(summaryReportService.page(query));
    }

    @GetMapping("/preview")
    @Operation(summary = "汇总报表预览")
    public ApiResponse<SummaryReportPreviewVO> preview(@Validated SummaryReportPreviewQuery query) {
        return ApiResponse.success(summaryReportService.preview(query));
    }

    @GetMapping("/export")
    @Operation(summary = "导出汇总报表列表")
    public ResponseEntity<byte[]> export(@Validated SummaryReportQuery query) {
        return summaryReportService.exportList(query);
    }

    @GetMapping("/detailExport")
    @Operation(summary = "导出汇总报表明细")
    public ResponseEntity<byte[]> detailExport(@Validated SummaryReportPreviewQuery query) {
        return summaryReportService.exportDetail(query);
    }
}
