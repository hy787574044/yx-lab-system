package com.yx.lab.modules.review.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.util.ExcelExportUtil;
import com.yx.lab.modules.review.dto.ReviewCommand;
import com.yx.lab.modules.review.dto.ReviewQuery;
import com.yx.lab.modules.review.entity.ReviewRecord;
import com.yx.lab.modules.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 审核流程控制器。
 * 负责审核记录查询与审核结果提交。
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "审核流程管理")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 分页查询审核记录。
     *
     * @param query 审核查询条件。
     * @return 审核记录分页结果。
     */
    @GetMapping
    @Operation(summary = "审核记录分页")
    public ApiResponse<PageResult<ReviewRecord>> page(@Validated ReviewQuery query) {
        return ApiResponse.success(reviewService.page(query));
    }

    /**
     * 导出审核记录。
     *
     * @param query 审核查询条件。
     * @return Excel 文件流。
     */
    @GetMapping("/export")
    @Operation(summary = "导出审核记录")
    public ResponseEntity<byte[]> export(@Validated ReviewQuery query) {
        ExcelExportUtil.prepareExportQuery(query);
        return ExcelExportUtil.buildResponse(
                "结果审查.xlsx",
                "结果审查",
                reviewService.page(query).getRecords(),
                java.util.Arrays.asList(
                        ExcelExportUtil.column("样品编号", ReviewRecord::getSampleNo),
                        ExcelExportUtil.column("封签编号", ReviewRecord::getSealNo),
                        ExcelExportUtil.column("检测流程ID", ReviewRecord::getDetectionRecordId),
                        ExcelExportUtil.column("审核人", ReviewRecord::getReviewerName),
                        ExcelExportUtil.column("审查状态", item -> LabWorkflowConstants.getReviewResultLabel(item.getReviewResult())),
                        ExcelExportUtil.column("审查时间", ReviewRecord::getReviewTime),
                        ExcelExportUtil.column("审核意见", ReviewRecord::getReviewRemark),
                        ExcelExportUtil.column("驳回原因", ReviewRecord::getRejectReason),
                        ExcelExportUtil.column("更新时间", ReviewRecord::getUpdatedTime)
                ));
    }

    /**
     * 提交审核结果。
     *
     * @param command 审核命令。
     * @return 提交结果。
     */
    @PostMapping
    @Operation(summary = "提交审核结果")
    public ApiResponse<Void> review(@Valid @RequestBody ReviewCommand command) {
        reviewService.review(command);
        return ApiResponse.successMessage("审核完成");
    }
}
