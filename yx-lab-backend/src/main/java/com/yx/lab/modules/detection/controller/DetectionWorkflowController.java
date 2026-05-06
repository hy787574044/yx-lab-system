package com.yx.lab.modules.detection.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.modules.detection.dto.DetectionRecordQuery;
import com.yx.lab.modules.detection.dto.DetectionSubmitCommand;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.service.DetectionWorkflowService;
import com.yx.lab.modules.detection.vo.DetectionRecordDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/detections")
@RequiredArgsConstructor
public class DetectionWorkflowController {

    private final DetectionWorkflowService detectionWorkflowService;

    @GetMapping
    public ApiResponse<PageResult<DetectionRecord>> page(@Validated DetectionRecordQuery query) {
        return ApiResponse.success(detectionWorkflowService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<DetectionRecordDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(detectionWorkflowService.detail(id));
    }

    @PostMapping("/submit")
    public ApiResponse<Void> submit(@Valid @RequestBody DetectionSubmitCommand command) {
        detectionWorkflowService.submit(command);
        return ApiResponse.successMessage("检测提交成功");
    }
}
