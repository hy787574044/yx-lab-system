package com.yx.lab.modules.review.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.modules.review.dto.ReviewCommand;
import com.yx.lab.modules.review.dto.ReviewQuery;
import com.yx.lab.modules.review.entity.ReviewRecord;
import com.yx.lab.modules.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
