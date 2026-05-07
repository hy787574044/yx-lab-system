package com.yx.lab.modules.report.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageQuery;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.modules.report.dto.ReportQuery;
import com.yx.lab.modules.report.dto.ReportTemplateSaveCommand;
import com.yx.lab.modules.report.entity.LabReport;
import com.yx.lab.modules.report.entity.ReportTemplate;
import com.yx.lab.modules.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.nio.charset.StandardCharsets;

/**
 * 报告控制器。
 * 负责报告查询、模板维护、报告发布与预览。
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "报告管理")
public class ReportController {

    private final ReportService reportService;

    /**
     * 分页查询报告。
     *
     * @param query 报告查询条件。
     * @return 报告分页结果。
     */
    @GetMapping
    @Operation(summary = "报告分页")
    public ApiResponse<PageResult<LabReport>> page(@Validated ReportQuery query) {
        return ApiResponse.success(reportService.page(query));
    }

    /**
     * 预览正式报告。
     *
     * @param id 报告主键。
     * @return 报告预览文件流。
     */
    @GetMapping("/{id}/preview")
    @Operation(summary = "预览正式报告")
    public ResponseEntity<byte[]> preview(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.builder("inline")
                                .filename("report-" + id + ".html", StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(MediaType.TEXT_HTML)
                .body(reportService.preview(id));
    }

    /**
     * 分页查询报告模板。
     *
     * @param query 分页查询参数。
     * @return 报告模板分页结果。
     */
    @GetMapping("/templates")
    @Operation(summary = "报告模板分页")
    public ApiResponse<PageResult<ReportTemplate>> templatePage(@Validated PageQuery query) {
        return ApiResponse.success(reportService.templatePage(query));
    }

    /**
     * 新增报告模板。
     *
     * @param command 模板保存命令。
     * @return 保存结果。
     */
    @PostMapping("/templates")
    @Operation(summary = "新增报告模板")
    public ApiResponse<Void> saveTemplate(@Valid @RequestBody ReportTemplateSaveCommand command) {
        reportService.saveTemplate(command);
        return ApiResponse.successMessage("新增成功");
    }

    /**
     * 更新报告模板。
     *
     * @param id 模板主键。
     * @param command 模板保存命令。
     * @return 更新结果。
     */
    @PutMapping("/templates/{id}")
    @Operation(summary = "更新报告模板")
    public ApiResponse<Void> updateTemplate(@PathVariable Long id, @Valid @RequestBody ReportTemplateSaveCommand command) {
        reportService.updateTemplate(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    /**
     * 删除报告模板。
     *
     * @param id 模板主键。
     * @return 删除结果。
     */
    @DeleteMapping("/templates/{id}")
    @Operation(summary = "删除报告模板")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id) {
        reportService.deleteTemplate(id);
        return ApiResponse.successMessage("删除成功");
    }

    /**
     * 发布正式报告。
     *
     * @param id 报告主键。
     * @return 发布结果。
     */
    @PostMapping("/{id}/publish")
    @Operation(summary = "发布正式报告")
    public ApiResponse<Void> publish(@PathVariable Long id) {
        reportService.publish(id);
        return ApiResponse.successMessage("发布成功");
    }

    /**
     * 取消发布正式报告。
     *
     * @param id 报告主键。
     * @return 取消发布结果。
     */
    @PostMapping("/{id}/unpublish")
    @Operation(summary = "取消发布报告")
    public ApiResponse<Void> unpublish(@PathVariable Long id) {
        reportService.unpublish(id);
        return ApiResponse.successMessage("取消发布成功");
    }
}
