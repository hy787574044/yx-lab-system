package com.yx.lab.modules.report.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageQuery;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.modules.report.dto.ReportQuery;
import com.yx.lab.modules.report.dto.ReportTemplateSaveCommand;
import com.yx.lab.modules.report.entity.LabReport;
import com.yx.lab.modules.report.entity.ReportTemplate;
import com.yx.lab.modules.report.service.ReportService;
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
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public ApiResponse<PageResult<LabReport>> page(@Validated ReportQuery query) {
        return ApiResponse.success(reportService.page(query));
    }

    @GetMapping("/templates")
    public ApiResponse<PageResult<ReportTemplate>> templatePage(@Validated PageQuery query) {
        return ApiResponse.success(reportService.templatePage(query));
    }

    @PostMapping("/templates")
    public ApiResponse<Void> saveTemplate(@Valid @RequestBody ReportTemplateSaveCommand command) {
        reportService.saveTemplate(command);
        return ApiResponse.successMessage("新增成功");
    }

    @PutMapping("/templates/{id}")
    public ApiResponse<Void> updateTemplate(@PathVariable Long id, @Valid @RequestBody ReportTemplateSaveCommand command) {
        reportService.updateTemplate(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    @DeleteMapping("/templates/{id}")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id) {
        reportService.deleteTemplate(id);
        return ApiResponse.successMessage("删除成功");
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publish(@PathVariable Long id) {
        reportService.publish(id);
        return ApiResponse.successMessage("发布成功");
    }

    @PostMapping("/{id}/unpublish")
    public ApiResponse<Void> unpublish(@PathVariable Long id) {
        reportService.unpublish(id);
        return ApiResponse.successMessage("取消发布成功");
    }
}
