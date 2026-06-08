package com.yx.lab.modules.sample.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.security.PermissionConstants;
import com.yx.lab.common.security.RequirePermission;
import com.yx.lab.common.util.ExcelExportUtil;
import cn.hutool.core.util.StrUtil;
import com.yx.lab.modules.sample.dto.LabSampleQuery;
import com.yx.lab.modules.sample.dto.SampleLoginCommand;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.service.LabSampleService;
import com.yx.lab.modules.sample.vo.StatusCountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 样品控制器。
 * 负责样品分页查询、详情查询和样品登录。
 */
@RestController
@RequestMapping("/api/samples")
@RequiredArgsConstructor
@Tag(name = "样品管理")
@RequirePermission(PermissionConstants.SAMPLE_VIEW)
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
     * 统计样品状态数量。
     *
     * @return 状态数量列表。
     */
    @GetMapping("/stats")
    @Operation(summary = "样品状态统计")
    public ApiResponse<List<StatusCountVO>> stats() {
        return ApiResponse.success(labSampleService.statusStats());
    }

    /**
     * 导出样品台账。
     *
     * @param query 样品查询条件。
     * @return Excel 文件流。
     */
    @GetMapping("/export")
    @Operation(summary = "导出样品台账")
    public ResponseEntity<byte[]> export(@Validated LabSampleQuery query) {
        ExcelExportUtil.prepareExportQuery(query);
        return ExcelExportUtil.buildResponse(
                "样品台账.xlsx",
                        "样品台账",
                        labSampleService.page(query).getRecords(),
                        java.util.Arrays.asList(
                        ExcelExportUtil.column("样品编号", LabSample::getSampleNo),
                        ExcelExportUtil.column("点位名称", LabSample::getPointName),
                        ExcelExportUtil.column("样品类型", item -> LabWorkflowConstants.getSampleTypeLabel(item.getSampleType())),
                        ExcelExportUtil.column("样品来源", item -> LabWorkflowConstants.getSampleSourceMethodLabel(item.getSampleSourceMethod())),
                        ExcelExportUtil.column("样品状态", item -> LabWorkflowConstants.getSampleStatusLabel(item.getSampleStatus())),
                        ExcelExportUtil.column("采样时间", LabSample::getSamplingTime),
                        ExcelExportUtil.column("采样人员", LabSample::getSamplerName),
                        ExcelExportUtil.column("审核流程", item -> StrUtil.blankToDefault(item.getReviewFlowName(), "-")),
                        ExcelExportUtil.column("保存条件", LabSample::getStorageCondition),
                        ExcelExportUtil.column("结果摘要", item -> LabWorkflowConstants.translateWorkflowText(item.getResultSummary())),
                        ExcelExportUtil.column("备注", LabSample::getRemark),
                        ExcelExportUtil.column("更新时间", LabSample::getUpdatedTime)
                ));
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
    @RequirePermission(PermissionConstants.SAMPLE_WRITE)
    public ApiResponse<LabSample> login(@Valid @RequestBody SampleLoginCommand command) {
        return ApiResponse.success("样品登录成功", labSampleService.loginSample(command));
    }
}
