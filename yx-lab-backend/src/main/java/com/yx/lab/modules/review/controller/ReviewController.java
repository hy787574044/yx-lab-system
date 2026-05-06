package com.yx.lab.modules.review.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.modules.review.dto.ReviewCommand;
import com.yx.lab.modules.review.dto.ReviewQuery;
import com.yx.lab.modules.review.entity.ReviewRecord;
import com.yx.lab.modules.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ApiResponse<PageResult<ReviewRecord>> page(@Validated ReviewQuery query) {
        return ApiResponse.success(reviewService.page(query));
    }

    @PostMapping
    public ApiResponse<Void> review(@Valid @RequestBody ReviewCommand command) {
        reviewService.review(command);
        return ApiResponse.successMessage("审核完成");
    }
}
