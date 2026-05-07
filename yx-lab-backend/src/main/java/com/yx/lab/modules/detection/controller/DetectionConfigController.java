package com.yx.lab.modules.detection.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.modules.detection.dto.DetectionParameterSaveCommand;
import com.yx.lab.modules.detection.dto.DetectionParameterQuery;
import com.yx.lab.modules.detection.dto.DetectionStepSaveCommand;
import com.yx.lab.modules.detection.dto.DetectionStepQuery;
import com.yx.lab.modules.detection.dto.DetectionTypeSaveCommand;
import com.yx.lab.modules.detection.dto.DetectionTypeQuery;
import com.yx.lab.modules.detection.entity.DetectionParameter;
import com.yx.lab.modules.detection.entity.DetectionStep;
import com.yx.lab.modules.detection.entity.DetectionType;
import com.yx.lab.modules.detection.service.DetectionConfigService;
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
@RequestMapping("/api/detection-config")
@RequiredArgsConstructor
public class DetectionConfigController {

    private final DetectionConfigService detectionConfigService;

    @GetMapping("/types")
    public ApiResponse<PageResult<DetectionType>> typePage(@Validated DetectionTypeQuery query) {
        return ApiResponse.success(detectionConfigService.typePage(query));
    }

    @PostMapping("/types")
    public ApiResponse<Void> saveType(@Valid @RequestBody DetectionTypeSaveCommand command) {
        detectionConfigService.saveType(command);
        return ApiResponse.successMessage("新增成功");
    }

    @PutMapping("/types/{id}")
    public ApiResponse<Void> updateType(@PathVariable Long id, @Valid @RequestBody DetectionTypeSaveCommand command) {
        detectionConfigService.updateType(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    @DeleteMapping("/types/{id}")
    public ApiResponse<Void> deleteType(@PathVariable Long id) {
        detectionConfigService.deleteType(id);
        return ApiResponse.successMessage("删除成功");
    }

    @GetMapping("/parameters")
    public ApiResponse<PageResult<DetectionParameter>> parameterPage(@Validated DetectionParameterQuery query) {
        return ApiResponse.success(detectionConfigService.parameterPage(query));
    }

    @PostMapping("/parameters")
    public ApiResponse<Void> saveParameter(@Valid @RequestBody DetectionParameterSaveCommand command) {
        detectionConfigService.saveParameter(command);
        return ApiResponse.successMessage("新增成功");
    }

    @PutMapping("/parameters/{id}")
    public ApiResponse<Void> updateParameter(@PathVariable Long id, @Valid @RequestBody DetectionParameterSaveCommand command) {
        detectionConfigService.updateParameter(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    @DeleteMapping("/parameters/{id}")
    public ApiResponse<Void> deleteParameter(@PathVariable Long id) {
        detectionConfigService.deleteParameter(id);
        return ApiResponse.successMessage("删除成功");
    }

    @GetMapping("/steps")
    public ApiResponse<PageResult<DetectionStep>> stepPage(@Validated DetectionStepQuery query) {
        return ApiResponse.success(detectionConfigService.stepPage(query));
    }

    @PostMapping("/steps")
    public ApiResponse<Void> saveStep(@Valid @RequestBody DetectionStepSaveCommand command) {
        detectionConfigService.saveStep(command);
        return ApiResponse.successMessage("新增成功");
    }

    @PutMapping("/steps/{id}")
    public ApiResponse<Void> updateStep(@PathVariable Long id, @Valid @RequestBody DetectionStepSaveCommand command) {
        detectionConfigService.updateStep(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    @DeleteMapping("/steps/{id}")
    public ApiResponse<Void> deleteStep(@PathVariable Long id) {
        detectionConfigService.deleteStep(id);
        return ApiResponse.successMessage("删除成功");
    }

}
