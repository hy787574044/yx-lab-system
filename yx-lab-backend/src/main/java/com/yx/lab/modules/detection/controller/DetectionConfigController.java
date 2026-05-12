package com.yx.lab.modules.detection.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.ExcelExportUtil;
import com.yx.lab.modules.detection.dto.DetectionMethodQuery;
import com.yx.lab.modules.detection.dto.DetectionMethodSaveCommand;
import com.yx.lab.modules.detection.dto.DetectionParameterMethodBindCommand;
import com.yx.lab.modules.detection.dto.DetectionParameterQuery;
import com.yx.lab.modules.detection.dto.DetectionParameterSaveCommand;
import com.yx.lab.modules.detection.dto.DetectionProjectGroupQuery;
import com.yx.lab.modules.detection.dto.DetectionProjectGroupSaveCommand;
import com.yx.lab.modules.detection.dto.DetectionStepQuery;
import com.yx.lab.modules.detection.dto.DetectionStepSaveCommand;
import com.yx.lab.modules.detection.dto.DetectionTypeQuery;
import com.yx.lab.modules.detection.dto.DetectionTypeSaveCommand;
import com.yx.lab.modules.detection.entity.DetectionMethod;
import com.yx.lab.modules.detection.entity.DetectionParameter;
import com.yx.lab.modules.detection.entity.DetectionProjectGroup;
import com.yx.lab.modules.detection.entity.DetectionStep;
import com.yx.lab.modules.detection.entity.DetectionType;
import com.yx.lab.modules.detection.service.DetectionConfigService;
import com.yx.lab.modules.detection.vo.DetectionDetectorOptionVO;
import com.yx.lab.modules.detection.vo.DetectionParameterMethodBindingVO;
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
 * 检测配置控制器。
 * 负责检测套餐、检测参数、检测方法、检测步骤等基础配置维护。
 */
@RestController
@RequestMapping("/api/detectionConfig")
@RequiredArgsConstructor
@Tag(name = "检测配置管理")
public class DetectionConfigController {

    private final DetectionConfigService detectionConfigService;

    /**
     * 分页查询检测套餐。
     *
     * @param query 检测套餐查询条件。
     * @return 检测套餐分页结果。
     */
    @GetMapping("/types")
    @Operation(summary = "检测套餐分页")
    public ApiResponse<PageResult<DetectionType>> typePage(@Validated DetectionTypeQuery query) {
        return ApiResponse.success(detectionConfigService.typePage(query));
    }

    /**
     * 导出检测套餐。
     *
     * @param query 检测套餐查询条件。
     * @return Excel 文件流。
     */
    @GetMapping("/types/export")
    @Operation(summary = "导出检测套餐")
    public ResponseEntity<byte[]> exportTypes(@Validated DetectionTypeQuery query) {
        ExcelExportUtil.prepareExportQuery(query);
        return ExcelExportUtil.buildResponse(
                "检测套餐.xlsx",
                "检测套餐",
                detectionConfigService.typePage(query).getRecords(),
                java.util.Arrays.asList(
                        ExcelExportUtil.column("套餐名称", DetectionType::getTypeName),
                        ExcelExportUtil.column("组内参数", DetectionType::getParameterNames),
                        ExcelExportUtil.column("参数检测方法", DetectionType::getParameterMethodNames),
                        ExcelExportUtil.column("状态", item -> item.getEnabled() != null && item.getEnabled() == 1 ? "启用" : "停用"),
                        ExcelExportUtil.column("备注", DetectionType::getRemark),
                        ExcelExportUtil.column("更新时间", DetectionType::getUpdatedTime)
                ));
    }

    /**
     * 新增检测套餐。
     *
     * @param command 检测套餐保存命令。
     * @return 保存结果。
     */
    @PostMapping("/types")
    @Operation(summary = "新增检测套餐")
    public ApiResponse<Void> saveType(@Valid @RequestBody DetectionTypeSaveCommand command) {
        detectionConfigService.saveType(command);
        return ApiResponse.successMessage("新增成功");
    }

    /**
     * 更新检测套餐。
     *
     * @param id 检测套餐主键。
     * @param command 检测套餐保存命令。
     * @return 更新结果。
     */
    @PostMapping("/types/{id}")
    @Operation(summary = "更新检测套餐")
    public ApiResponse<Void> updateType(@PathVariable Long id, @Valid @RequestBody DetectionTypeSaveCommand command) {
        detectionConfigService.updateType(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    /**
     * 删除检测套餐。
     *
     * @param id 检测套餐主键。
     * @return 删除结果。
     */
    @PostMapping("/types/{id}/delete")
    @Operation(summary = "删除检测套餐")
    public ApiResponse<Void> deleteType(@PathVariable Long id) {
        detectionConfigService.deleteType(id);
        return ApiResponse.successMessage("删除成功");
    }

    /**
     * 获取检测员下拉选项。
     *
     * @return 检测员选项列表。
     */
    @GetMapping("/detectors")
    @Operation(summary = "获取检测员选项")
    public ApiResponse<List<DetectionDetectorOptionVO>> detectorOptions() {
        return ApiResponse.success(detectionConfigService.detectorOptions());
    }

    /**
     * 分页查询检测项目组。
     *
     * @param query 检测项目组查询条件。
     * @return 检测项目组分页结果。
     */
    @GetMapping("/projectGroups")
    @Operation(summary = "检测项目组分页")
    public ApiResponse<PageResult<DetectionProjectGroup>> projectGroupPage(@Validated DetectionProjectGroupQuery query) {
        return ApiResponse.success(detectionConfigService.projectGroupPage(query));
    }

    /**
     * 新增检测项目组。
     *
     * @param command 检测项目组保存命令。
     * @return 保存结果。
     */
    @PostMapping("/projectGroups")
    @Operation(summary = "新增检测项目组")
    public ApiResponse<Void> saveProjectGroup(@Valid @RequestBody DetectionProjectGroupSaveCommand command) {
        detectionConfigService.saveProjectGroup(command);
        return ApiResponse.successMessage("新增成功");
    }

    /**
     * 更新检测项目组。
     *
     * @param id 检测项目组主键。
     * @param command 检测项目组保存命令。
     * @return 更新结果。
     */
    @PostMapping("/projectGroups/{id}")
    @Operation(summary = "更新检测项目组")
    public ApiResponse<Void> updateProjectGroup(@PathVariable Long id, @Valid @RequestBody DetectionProjectGroupSaveCommand command) {
        detectionConfigService.updateProjectGroup(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    /**
     * 删除检测项目组。
     *
     * @param id 检测项目组主键。
     * @return 删除结果。
     */
    @PostMapping("/projectGroups/{id}/delete")
    @Operation(summary = "删除检测项目组")
    public ApiResponse<Void> deleteProjectGroup(@PathVariable Long id) {
        detectionConfigService.deleteProjectGroup(id);
        return ApiResponse.successMessage("删除成功");
    }

    /**
     * 分页查询检测参数。
     *
     * @param query 检测参数查询条件。
     * @return 检测参数分页结果。
     */
    @GetMapping("/parameters")
    @Operation(summary = "检测参数分页")
    public ApiResponse<PageResult<DetectionParameter>> parameterPage(@Validated DetectionParameterQuery query) {
        return ApiResponse.success(detectionConfigService.parameterPage(query));
    }

    /**
     * 导出检测参数。
     *
     * @param query 检测参数查询条件。
     * @return Excel 文件流。
     */
    @GetMapping("/parameters/export")
    @Operation(summary = "导出检测参数")
    public ResponseEntity<byte[]> exportParameters(@Validated DetectionParameterQuery query) {
        ExcelExportUtil.prepareExportQuery(query);
        return ExcelExportUtil.buildResponse(
                "检测参数.xlsx",
                "检测参数",
                detectionConfigService.parameterPage(query).getRecords(),
                java.util.Arrays.asList(
                        ExcelExportUtil.column("参数名称", DetectionParameter::getParameterName),
                        ExcelExportUtil.column("单位", DetectionParameter::getUnit),
                        ExcelExportUtil.column("标准下限", DetectionParameter::getStandardMin),
                        ExcelExportUtil.column("标准上限", DetectionParameter::getStandardMax),
                        ExcelExportUtil.column("参考标准", DetectionParameter::getReferenceStandard),
                        ExcelExportUtil.column("判定规则", DetectionParameter::getExceedRule),
                        ExcelExportUtil.column("状态", item -> item.getEnabled() != null && item.getEnabled() == 1 ? "启用" : "停用"),
                        ExcelExportUtil.column("备注", DetectionParameter::getRemark),
                        ExcelExportUtil.column("更新时间", DetectionParameter::getUpdatedTime)
                ));
    }

    /**
     * 新增检测参数。
     *
     * @param command 检测参数保存命令。
     * @return 保存结果。
     */
    @PostMapping("/parameters")
    @Operation(summary = "新增检测参数")
    public ApiResponse<Void> saveParameter(@Valid @RequestBody DetectionParameterSaveCommand command) {
        detectionConfigService.saveParameter(command);
        return ApiResponse.successMessage("新增成功");
    }

    /**
     * 更新检测参数。
     *
     * @param id 检测参数主键。
     * @param command 检测参数保存命令。
     * @return 更新结果。
     */
    @PostMapping("/parameters/{id}")
    @Operation(summary = "更新检测参数")
    public ApiResponse<Void> updateParameter(@PathVariable Long id, @Valid @RequestBody DetectionParameterSaveCommand command) {
        detectionConfigService.updateParameter(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    /**
     * 删除检测参数。
     *
     * @param id 检测参数主键。
     * @return 删除结果。
     */
    @PostMapping("/parameters/{id}/delete")
    @Operation(summary = "删除检测参数")
    public ApiResponse<Void> deleteParameter(@PathVariable Long id) {
        detectionConfigService.deleteParameter(id);
        return ApiResponse.successMessage("删除成功");
    }

    /**
     * 分页查询检测参数与检测方法绑定关系。
     *
     * @param query 检测参数查询条件。
     * @return 绑定关系分页结果。
     */
    @GetMapping("/parameterMethodBindings")
    @Operation(summary = "检测参数方法绑定分页")
    public ApiResponse<PageResult<DetectionParameterMethodBindingVO>> parameterMethodBindingPage(@Validated DetectionParameterQuery query) {
        return ApiResponse.success(detectionConfigService.parameterMethodBindingPage(query));
    }

    /**
     * 保存单个检测参数对应的检测方法绑定关系。
     *
     * @param parameterId 检测参数主键。
     * @param command 绑定保存命令。
     * @return 保存结果。
     */
    @PostMapping("/parameterMethodBindings/{parameterId}")
    @Operation(summary = "保存检测参数方法绑定")
    public ApiResponse<Void> bindParameterMethods(@PathVariable Long parameterId,
                                                  @RequestBody(required = false) DetectionParameterMethodBindCommand command) {
        detectionConfigService.bindParameterMethods(parameterId, command);
        return ApiResponse.successMessage("保存成功");
    }

    /**
     * 分页查询检测方法。
     *
     * @param query 检测方法查询条件。
     * @return 检测方法分页结果。
     */
    @GetMapping("/methods")
    @Operation(summary = "检测方法分页")
    public ApiResponse<PageResult<DetectionMethod>> methodPage(@Validated DetectionMethodQuery query) {
        return ApiResponse.success(detectionConfigService.methodPage(query));
    }

    /**
     * 导出检测方法。
     *
     * @param query 检测方法查询条件。
     * @return Excel 文件流。
     */
    @GetMapping("/methods/export")
    @Operation(summary = "导出检测方法")
    public ResponseEntity<byte[]> exportMethods(@Validated DetectionMethodQuery query) {
        ExcelExportUtil.prepareExportQuery(query);
        return ExcelExportUtil.buildResponse(
                "检测方法.xlsx",
                "检测方法",
                detectionConfigService.methodPage(query).getRecords(),
                java.util.Arrays.asList(
                        ExcelExportUtil.column("检测方法名称", DetectionMethod::getMethodName),
                        ExcelExportUtil.column("方法编码", DetectionMethod::getMethodCode),
                        ExcelExportUtil.column("标准编号", DetectionMethod::getStandardCode),
                        ExcelExportUtil.column("已绑定参数", DetectionMethod::getParameterName),
                        ExcelExportUtil.column("检测依据", DetectionMethod::getMethodBasis),
                        ExcelExportUtil.column("适用范围", DetectionMethod::getApplyScope),
                        ExcelExportUtil.column("状态", item -> item.getEnabled() != null && item.getEnabled() == 1 ? "启用" : "停用"),
                        ExcelExportUtil.column("备注", DetectionMethod::getRemark),
                        ExcelExportUtil.column("更新时间", DetectionMethod::getUpdatedTime)
                ));
    }

    /**
     * 获取检测方法下拉选项。
     *
     * @return 检测方法列表。
     */
    @GetMapping("/methods/options")
    @Operation(summary = "获取检测方法选项")
    public ApiResponse<List<DetectionMethod>> methodOptions() {
        return ApiResponse.success(detectionConfigService.methodOptions());
    }

    /**
     * 新增检测方法。
     *
     * @param command 检测方法保存命令。
     * @return 保存结果。
     */
    @PostMapping("/methods")
    @Operation(summary = "新增检测方法")
    public ApiResponse<Void> saveMethod(@Valid @RequestBody DetectionMethodSaveCommand command) {
        detectionConfigService.saveMethod(command);
        return ApiResponse.successMessage("新增成功");
    }

    /**
     * 更新检测方法。
     *
     * @param id 检测方法主键。
     * @param command 检测方法保存命令。
     * @return 更新结果。
     */
    @PostMapping("/methods/{id}")
    @Operation(summary = "更新检测方法")
    public ApiResponse<Void> updateMethod(@PathVariable Long id, @Valid @RequestBody DetectionMethodSaveCommand command) {
        detectionConfigService.updateMethod(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    /**
     * 删除检测方法。
     *
     * @param id 检测方法主键。
     * @return 删除结果。
     */
    @PostMapping("/methods/{id}/delete")
    @Operation(summary = "删除检测方法")
    public ApiResponse<Void> deleteMethod(@PathVariable Long id) {
        detectionConfigService.deleteMethod(id);
        return ApiResponse.successMessage("删除成功");
    }

    /**
     * 分页查询检测步骤。
     *
     * @param query 检测步骤查询条件。
     * @return 检测步骤分页结果。
     */
    @GetMapping("/steps")
    @Operation(summary = "检测步骤分页")
    public ApiResponse<PageResult<DetectionStep>> stepPage(@Validated DetectionStepQuery query) {
        return ApiResponse.success(detectionConfigService.stepPage(query));
    }

    /**
     * 新增检测步骤。
     *
     * @param command 检测步骤保存命令。
     * @return 保存结果。
     */
    @PostMapping("/steps")
    @Operation(summary = "新增检测步骤")
    public ApiResponse<Void> saveStep(@Valid @RequestBody DetectionStepSaveCommand command) {
        detectionConfigService.saveStep(command);
        return ApiResponse.successMessage("新增成功");
    }

    /**
     * 更新检测步骤。
     *
     * @param id 检测步骤主键。
     * @param command 检测步骤保存命令。
     * @return 更新结果。
     */
    @PostMapping("/steps/{id}")
    @Operation(summary = "更新检测步骤")
    public ApiResponse<Void> updateStep(@PathVariable Long id, @Valid @RequestBody DetectionStepSaveCommand command) {
        detectionConfigService.updateStep(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    /**
     * 删除检测步骤。
     *
     * @param id 检测步骤主键。
     * @return 删除结果。
     */
    @PostMapping("/steps/{id}/delete")
    @Operation(summary = "删除检测步骤")
    public ApiResponse<Void> deleteStep(@PathVariable Long id) {
        detectionConfigService.deleteStep(id);
        return ApiResponse.successMessage("删除成功");
    }
}
