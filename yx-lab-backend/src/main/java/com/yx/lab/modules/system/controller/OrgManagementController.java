package com.yx.lab.modules.system.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.modules.system.dto.OrgQuery;
import com.yx.lab.modules.system.dto.OrgSaveCommand;
import com.yx.lab.modules.system.service.OrgManagementService;
import com.yx.lab.modules.system.vo.LabOrgVO;
import com.yx.lab.modules.system.vo.OrgOptionVO;
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
import java.util.List;

/**
 * 机构管理控制器。
 */
@RestController
@RequestMapping("/api/system/orgs")
@RequiredArgsConstructor
@Tag(name = "系统管理-机构管理")
public class OrgManagementController {

    private final OrgManagementService orgManagementService;

    /**
     * 分页查询机构。
     *
     * @param query 查询条件
     * @return 机构分页结果
     */
    @GetMapping
    @Operation(summary = "分页查询机构")
    public ApiResponse<PageResult<LabOrgVO>> page(@Validated OrgQuery query) {
        return ApiResponse.success(orgManagementService.page(query));
    }

    /**
     * 获取机构详情。
     *
     * @param id 机构ID
     * @return 机构详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取机构详情")
    public ApiResponse<LabOrgVO> detail(@PathVariable Long id) {
        return ApiResponse.success(orgManagementService.detail(id));
    }

    /**
     * 获取启用机构选项。
     *
     * @return 机构下拉选项
     */
    @GetMapping("/options")
    @Operation(summary = "获取启用机构选项")
    public ApiResponse<List<OrgOptionVO>> options() {
        return ApiResponse.success(orgManagementService.options());
    }

    /**
     * 新增机构。
     *
     * @param command 机构保存命令
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "新增机构")
    public ApiResponse<Void> save(@Valid @RequestBody OrgSaveCommand command) {
        orgManagementService.save(command);
        return ApiResponse.successMessage("新增成功");
    }

    /**
     * 更新机构。
     *
     * @param id 机构ID
     * @param command 机构保存命令
     * @return 操作结果
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新机构")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody OrgSaveCommand command) {
        orgManagementService.update(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    /**
     * 删除机构。
     *
     * @param id 机构ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除机构")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        orgManagementService.delete(id);
        return ApiResponse.successMessage("删除成功");
    }
}
