package com.yx.lab.modules.system.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.ExcelExportUtil;
import com.yx.lab.modules.system.dto.UserQuery;
import com.yx.lab.modules.system.dto.UserSaveCommand;
import com.yx.lab.modules.system.service.UserManagementService;
import com.yx.lab.modules.system.vo.LabUserVO;
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

/**
 * 用户管理控制器。
 */
@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
@Tag(name = "系统管理-用户管理")
public class UserManagementController {

    private final UserManagementService userManagementService;

    /**
     * 分页查询用户。
     *
     * @param query 查询条件
     * @return 用户分页结果
     */
    @GetMapping
    @Operation(summary = "分页查询用户")
    public ApiResponse<PageResult<LabUserVO>> page(@Validated UserQuery query) {
        return ApiResponse.success(userManagementService.page(query));
    }

    /**
     * 导出用户列表。
     *
     * @param query 查询条件
     * @return Excel 文件流
     */
    @GetMapping("/export")
    @Operation(summary = "导出用户列表")
    public ResponseEntity<byte[]> export(@Validated UserQuery query) {
        ExcelExportUtil.prepareExportQuery(query);
        return ExcelExportUtil.buildResponse(
                "用户管理.xlsx",
                "用户管理",
                userManagementService.page(query).getRecords(),
                java.util.Arrays.asList(
                        ExcelExportUtil.column("用户名", LabUserVO::getUsername),
                        ExcelExportUtil.column("姓名", LabUserVO::getRealName),
                        ExcelExportUtil.column("所属机构", LabUserVO::getOrgName),
                        ExcelExportUtil.column("角色编码", LabUserVO::getRoleCode),
                        ExcelExportUtil.column("手机号", LabUserVO::getPhone),
                        ExcelExportUtil.column("状态", item -> item.getStatus() != null && item.getStatus() == 1 ? "启用" : "停用"),
                        ExcelExportUtil.column("创建人", LabUserVO::getCreatedName),
                        ExcelExportUtil.column("创建时间", LabUserVO::getCreatedTime),
                        ExcelExportUtil.column("更新人", LabUserVO::getUpdatedName),
                        ExcelExportUtil.column("更新时间", LabUserVO::getUpdatedTime)
                ));
    }

    /**
     * 获取用户详情。
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情")
    public ApiResponse<LabUserVO> detail(@PathVariable Long id) {
        return ApiResponse.success(userManagementService.detail(id));
    }

    /**
     * 新增用户。
     *
     * @param command 用户保存命令
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "新增用户")
    public ApiResponse<Void> save(@Valid @RequestBody UserSaveCommand command) {
        userManagementService.save(command);
        return ApiResponse.successMessage("新增成功");
    }

    /**
     * 更新用户。
     *
     * @param id 用户ID
     * @param command 用户保存命令
     * @return 操作结果
     */
    @PostMapping("/{id}")
    @Operation(summary = "更新用户")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody UserSaveCommand command) {
        userManagementService.update(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    /**
     * 删除用户。
     *
     * @param id 用户ID
     * @return 操作结果
     */
    @PostMapping("/{id}/delete")
    @Operation(summary = "删除用户")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userManagementService.delete(id);
        return ApiResponse.successMessage("删除成功");
    }
}
