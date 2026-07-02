package com.yx.lab.modules.system.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.PermissionConstants;
import com.yx.lab.common.security.RequirePermission;
import com.yx.lab.modules.system.dto.FlowConfigQuery;
import com.yx.lab.modules.system.dto.FlowConfigSaveCommand;
import com.yx.lab.modules.system.dto.FlowNodeCommand;
import com.yx.lab.modules.system.service.FlowConfigManagementService;
import com.yx.lab.modules.system.vo.FlowConfigOptionVO;
import com.yx.lab.modules.system.vo.FlowConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 流程配置管理控制器。
 */
@RestController
@RequestMapping("/api/system/flowConfigs")
@RequiredArgsConstructor
@Tag(name = "系统管理-流程配置")
@RequirePermission(PermissionConstants.SYSTEM_VIEW)
public class FlowConfigManagementController {

    private final FlowConfigManagementService flowConfigManagementService;

    /**
     * 分页查询流程配置。
     *
     * @param query 查询条件
     * @return 流程配置分页结果
     */
    @GetMapping
    @Operation(summary = "分页查询流程配置")
    public ApiResponse<PageResult<FlowConfigVO>> page(@Validated FlowConfigQuery query) {
        return ApiResponse.success(flowConfigManagementService.page(query));
    }

    /**
     * 获取流程详情。
     *
     * @param id 流程ID
     * @return 流程详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取流程配置详情")
    public ApiResponse<FlowConfigVO> detail(@PathVariable Long id) {
        return ApiResponse.success(flowConfigManagementService.detail(id));
    }

    /**
     * 获取启用流程下拉选项。
     *
     * @param flowType 流程类型
     * @return 流程下拉选项
     */
    @GetMapping("/options")
    @Operation(summary = "获取启用流程下拉选项")
    @RequirePermission(value = {
            PermissionConstants.SAMPLE_VIEW,
            PermissionConstants.REVIEW_VIEW,
            PermissionConstants.REPORT_VIEW,
            PermissionConstants.SYSTEM_VIEW
    }, any = true)
    public ApiResponse<List<FlowConfigOptionVO>> options(@RequestParam(required = false) String flowType) {
        return ApiResponse.success(flowConfigManagementService.options(flowType));
    }

    /**
     * 新增流程配置。
     *
     * @param command 保存命令
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "新增流程配置")
    @RequirePermission(PermissionConstants.SYSTEM_WRITE)
    public ApiResponse<Void> save(@Valid @RequestBody FlowConfigSaveCommand command) {
        flowConfigManagementService.save(command);
        return ApiResponse.successMessage("新增成功");
    }

    /**
     * 更新流程配置。
     *
     * @param id 流程ID
     * @param command 保存命令
     * @return 操作结果
     */
    @PostMapping("/{id}")
    @Operation(summary = "更新流程配置")
    @RequirePermission(PermissionConstants.SYSTEM_WRITE)
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody FlowConfigSaveCommand command) {
        flowConfigManagementService.update(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    /**
     * 保存流程节点。
     *
     * @param id 流程ID
     * @param nodes 节点列表
     * @return 操作结果
     */
    @PostMapping("/{id}/nodes")
    @Operation(summary = "保存流程节点")
    @RequirePermission(PermissionConstants.SYSTEM_WRITE)
    public ApiResponse<Void> saveNodes(@PathVariable Long id, @Valid @RequestBody List<FlowNodeCommand> nodes) {
        flowConfigManagementService.saveNodes(id, nodes);
        return ApiResponse.successMessage("节点保存成功");
    }

    /**
     * 更新流程启停状态。
     *
     * @param id 流程ID
     * @param command 状态命令
     * @return 操作结果
     */
    @PostMapping("/{id}/status")
    @Operation(summary = "更新流程启停状态")
    @RequirePermission(PermissionConstants.SYSTEM_WRITE)
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody FlowStatusCommand command) {
        flowConfigManagementService.updateStatus(id, command.getStatus());
        return ApiResponse.successMessage("状态更新成功");
    }

    /**
     * 设置默认流程。
     *
     * @param id 流程ID
     * @return 操作结果
     */
    @PostMapping("/{id}/default")
    @Operation(summary = "设置默认流程")
    @RequirePermission(PermissionConstants.SYSTEM_WRITE)
    public ApiResponse<Void> setDefault(@PathVariable Long id) {
        flowConfigManagementService.setDefault(id);
        return ApiResponse.successMessage("默认流程设置成功");
    }

    /**
     * 删除流程配置。
     *
     * @param id 流程ID
     * @return 操作结果
     */
    @PostMapping("/{id}/delete")
    @Operation(summary = "删除流程配置")
    @RequirePermission(PermissionConstants.SYSTEM_WRITE)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        flowConfigManagementService.delete(id);
        return ApiResponse.successMessage("删除成功");
    }

    @Data
    public static class FlowStatusCommand {

        @NotNull(message = "状态不能为空")
        private Integer status;
    }
}
