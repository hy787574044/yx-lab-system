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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "检测配置管理")
public class DetectionConfigController {

    private final DetectionConfigService detectionConfigService;

    @GetMapping("/types")
    @Operation(summary = "检测类型分页")
    public ApiResponse<PageResult<DetectionType>> typePage(@Validated DetectionTypeQuery query) {
        return ApiResponse.success(detectionConfigService.typePage(query));
    }

    @PostMapping("/types")
    @Operation(summary = "新增检测类型")
    public ApiResponse<Void> saveType(@Valid @RequestBody DetectionTypeSaveCommand command) {
        detectionConfigService.saveType(command);
        return ApiResponse.successMessage("新增成功");
    }

    @PutMapping("/types/{id}")
    @Operation(summary = "更新检测类型")
    public ApiResponse<Void> updateType(@PathVariable Long id, @Valid @RequestBody DetectionTypeSaveCommand command) {
        detectionConfigService.updateType(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    @DeleteMapping("/types/{id}")
    @Operation(summary = "删除检测类型")
    public ApiResponse<Void> deleteType(@PathVariable Long id) {
        detectionConfigService.deleteType(id);
        return ApiResponse.successMessage("删除成功");
    }

    @GetMapping("/parameters")
    @Operation(summary = "检测参数分页")
    public ApiResponse<PageResult<DetectionParameter>> parameterPage(@Validated DetectionParameterQuery query) {
        return ApiResponse.success(detectionConfigService.parameterPage(query));
    }

    @PostMapping("/parameters")
    @Operation(summary = "新增检测参数")
    public ApiResponse<Void> saveParameter(@Valid @RequestBody DetectionParameterSaveCommand command) {
        detectionConfigService.saveParameter(command);
        return ApiResponse.successMessage("新增成功");
    }

    @PutMapping("/parameters/{id}")
    @Operation(summary = "更新检测参数")
    public ApiResponse<Void> updateParameter(@PathVariable Long id, @Valid @RequestBody DetectionParameterSaveCommand command) {
        detectionConfigService.updateParameter(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    @DeleteMapping("/parameters/{id}")
    @Operation(summary = "删除检测参数")
    public ApiResponse<Void> deleteParameter(@PathVariable Long id) {
        detectionConfigService.deleteParameter(id);
        return ApiResponse.successMessage("删除成功");
    }

    @GetMapping("/steps")
    @Operation(summary = "检测步骤分页")
    public ApiResponse<PageResult<DetectionStep>> stepPage(@Validated DetectionStepQuery query) {
        return ApiResponse.success(detectionConfigService.stepPage(query));
    }

    @PostMapping("/steps")
    @Operation(summary = "新增检测步骤")
    public ApiResponse<Void> saveStep(@Valid @RequestBody DetectionStepSaveCommand command) {
        detectionConfigService.saveStep(command);
        return ApiResponse.successMessage("新增成功");
    }

    @PutMapping("/steps/{id}")
    @Operation(summary = "更新检测步骤")
    public ApiResponse<Void> updateStep(@PathVariable Long id, @Valid @RequestBody DetectionStepSaveCommand command) {
        detectionConfigService.updateStep(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    @DeleteMapping("/steps/{id}")
    @Operation(summary = "删除检测步骤")
    public ApiResponse<Void> deleteStep(@PathVariable Long id) {
        detectionConfigService.deleteStep(id);
        return ApiResponse.successMessage("删除成功");
    }

}
