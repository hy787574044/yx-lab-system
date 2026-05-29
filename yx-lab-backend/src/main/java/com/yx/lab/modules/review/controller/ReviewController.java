package com.yx.lab.modules.review.controller;

import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.PermissionConstants;
import com.yx.lab.common.security.RequirePermission;
import com.yx.lab.common.util.ExcelExportUtil;
import com.yx.lab.modules.review.dto.ReviewCommand;
import com.yx.lab.modules.review.dto.ReviewQuery;
import com.yx.lab.modules.review.entity.ReviewRecord;
import com.yx.lab.modules.review.service.ReviewService;
import com.yx.lab.modules.sample.vo.StatusCountVO;
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
import java.util.Arrays;
import java.util.List;

/**
 * 审核流程控制器。
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "审核流程管理")
@RequirePermission(PermissionConstants.REVIEW_VIEW)
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 分页查询审核记录。
     *
     * @param query 审核查询条件
     * @return 审核记录分页结果
     */
    @GetMapping
    @Operation(summary = "审核记录分页")
    public ApiResponse<PageResult<ReviewRecord>> page(@Validated ReviewQuery query) {
        return ApiResponse.success(reviewService.page(query));
    }

    @GetMapping("/stats")
    @Operation(summary = "审核状态统计")
    public ApiResponse<List<StatusCountVO>> stats() {
        return ApiResponse.success(reviewService.statusStats());
    }

    /**
     * 导出审核记录。
     *
     * @param query 审核查询条件
     * @return Excel 文件流
     */
    @GetMapping("/export")
    @Operation(summary = "导出审核记录")
    public ResponseEntity<byte[]> export(@Validated ReviewQuery query) {
        ExcelExportUtil.prepareExportQuery(query);
        return ExcelExportUtil.buildResponse(
                "结果审核.xlsx",
                "结果审核",
                reviewService.page(query).getRecords(),
                Arrays.asList(
                        ExcelExportUtil.column("样品编号", ReviewRecord::getSampleNo),
                        ExcelExportUtil.column("检测流程ID", ReviewRecord::getDetectionRecordId),
                        ExcelExportUtil.column("检测人员", ReviewRecord::getDetectorName),
                        ExcelExportUtil.column("审核人", ReviewRecord::getReviewerName),
                        ExcelExportUtil.column("审核状态", item -> LabWorkflowConstants.getReviewResultLabel(item.getReviewResult())),
                        ExcelExportUtil.column("审核时间", ReviewRecord::getReviewTime),
                        ExcelExportUtil.column("审核意见", ReviewRecord::getReviewRemark),
                        ExcelExportUtil.column("驳回原因", ReviewRecord::getRejectReason),
                        ExcelExportUtil.column("更新时间", ReviewRecord::getUpdatedTime)
                ));
    }

    /**
     * 提交审核结果。
     *
     * @param command 审核命令
     * @return 提交结果
     */
    @PostMapping
    @Operation(summary = "提交审核结果")
    @RequirePermission(PermissionConstants.REVIEW_AUDIT)
    public ApiResponse<Void> review(@Valid @RequestBody ReviewCommand command) {
        reviewService.review(command);
        return ApiResponse.successMessage("审核完成");
    }
}
