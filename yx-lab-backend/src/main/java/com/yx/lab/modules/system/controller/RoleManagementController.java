package com.yx.lab.modules.system.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.ExcelExportUtil;
import com.yx.lab.modules.system.dto.RoleQuery;
import com.yx.lab.modules.system.dto.RoleSaveCommand;
import com.yx.lab.modules.system.service.RoleManagementService;
import com.yx.lab.modules.system.vo.LabRoleVO;
import com.yx.lab.modules.system.vo.RoleOptionVO;
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
 * 角色管理控制器。
 */
@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
@Tag(name = "系统管理-角色管理")
public class RoleManagementController {

    private final RoleManagementService roleManagementService;

    /**
     * 分页查询角色。
     *
     * @param query 查询条件
     * @return 角色分页结果
     */
    @GetMapping
    @Operation(summary = "分页查询角色")
    public ApiResponse<PageResult<LabRoleVO>> page(@Validated RoleQuery query) {
        return ApiResponse.success(roleManagementService.page(query));
    }

    /**
     * 导出角色列表。
     *
     * @param query 查询条件
     * @return Excel 文件流
     */
    @GetMapping("/export")
    @Operation(summary = "导出角色列表")
    public ResponseEntity<byte[]> export(@Validated RoleQuery query) {
        ExcelExportUtil.prepareExportQuery(query);
        return ExcelExportUtil.buildResponse(
                "角色管理.xlsx",
                "角色管理",
                roleManagementService.page(query).getRecords(),
                java.util.Arrays.asList(
                        ExcelExportUtil.column("角色编码", LabRoleVO::getRoleCode),
                        ExcelExportUtil.column("角色名称", LabRoleVO::getRoleName),
                        ExcelExportUtil.column("适用范围", LabRoleVO::getRoleScope),
                        ExcelExportUtil.column("关联用户数", LabRoleVO::getUserCount),
                        ExcelExportUtil.column("状态", item -> item.getStatus() != null && item.getStatus() == 1 ? "启用" : "停用"),
                        ExcelExportUtil.column("备注", LabRoleVO::getRemark),
                        ExcelExportUtil.column("创建时间", LabRoleVO::getCreatedTime),
                        ExcelExportUtil.column("更新时间", LabRoleVO::getUpdatedTime)
                ));
    }

    /**
     * 获取角色详情。
     *
     * @param id 角色ID
     * @return 角色详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取角色详情")
    public ApiResponse<LabRoleVO> detail(@PathVariable Long id) {
        return ApiResponse.success(roleManagementService.detail(id));
    }

    /**
     * 获取启用角色选项。
     *
     * @return 角色下拉选项
     */
    @GetMapping("/options")
    @Operation(summary = "获取启用角色选项")
    public ApiResponse<List<RoleOptionVO>> options() {
        return ApiResponse.success(roleManagementService.options());
    }

    /**
     * 新增角色。
     *
     * @param command 角色保存命令
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "新增角色")
    public ApiResponse<Void> save(@Valid @RequestBody RoleSaveCommand command) {
        roleManagementService.save(command);
        return ApiResponse.successMessage("新增成功");
    }

    /**
     * 更新角色。
     *
     * @param id 角色ID
     * @param command 角色保存命令
     * @return 操作结果
     */
    @PostMapping("/{id}")
    @Operation(summary = "更新角色")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody RoleSaveCommand command) {
        roleManagementService.update(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    /**
     * 删除角色。
     *
     * @param id 角色ID
     * @return 操作结果
     */
    @PostMapping("/{id}/delete")
    @Operation(summary = "删除角色")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleManagementService.delete(id);
        return ApiResponse.successMessage("删除成功");
    }
}
