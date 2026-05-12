package com.yx.lab.modules.system.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.ExcelExportUtil;
import com.yx.lab.modules.system.dto.DictQuery;
import com.yx.lab.modules.system.dto.DictSaveCommand;
import com.yx.lab.modules.system.service.DictManagementService;
import com.yx.lab.modules.system.vo.LabDictVO;
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
 * 数据字典管理控制器。
 */
@RestController
@RequestMapping("/api/system/dicts")
@RequiredArgsConstructor
@Tag(name = "系统管理-数据字典管理")
public class DictManagementController {

    private final DictManagementService dictManagementService;

    /**
     * 分页查询数据字典。
     *
     * @param query 查询条件
     * @return 数据字典分页结果
     */
    @GetMapping
    @Operation(summary = "分页查询数据字典")
    public ApiResponse<PageResult<LabDictVO>> page(@Validated DictQuery query) {
        return ApiResponse.success(dictManagementService.page(query));
    }

    /**
     * 导出数据字典。
     *
     * @param query 查询条件
     * @return Excel 文件流
     */
    @GetMapping("/export")
    @Operation(summary = "导出数据字典")
    public ResponseEntity<byte[]> export(@Validated DictQuery query) {
        ExcelExportUtil.prepareExportQuery(query);
        return ExcelExportUtil.buildResponse(
                "数据字典.xlsx",
                "数据字典",
                dictManagementService.page(query).getRecords(),
                java.util.Arrays.asList(
                        ExcelExportUtil.column("字典编码", LabDictVO::getDictCode),
                        ExcelExportUtil.column("字典名称", LabDictVO::getDictName),
                        ExcelExportUtil.column("所属模块", LabDictVO::getModuleName),
                        ExcelExportUtil.column("字典项数", LabDictVO::getItemCount),
                        ExcelExportUtil.column("字典项内容", LabDictVO::getItemText),
                        ExcelExportUtil.column("状态", item -> item.getStatus() != null && item.getStatus() == 1 ? "启用" : "停用"),
                        ExcelExportUtil.column("备注", LabDictVO::getRemark),
                        ExcelExportUtil.column("更新时间", LabDictVO::getUpdatedTime)
                ));
    }

    /**
     * 获取数据字典详情。
     *
     * @param id 字典ID
     * @return 数据字典详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取数据字典详情")
    public ApiResponse<LabDictVO> detail(@PathVariable Long id) {
        return ApiResponse.success(dictManagementService.detail(id));
    }

    /**
     * 新增数据字典。
     *
     * @param command 保存命令
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "新增数据字典")
    public ApiResponse<Void> save(@Valid @RequestBody DictSaveCommand command) {
        dictManagementService.save(command);
        return ApiResponse.successMessage("新增成功");
    }

    /**
     * 更新数据字典。
     *
     * @param id 字典ID
     * @param command 保存命令
     * @return 操作结果
     */
    @PostMapping("/{id}")
    @Operation(summary = "更新数据字典")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody DictSaveCommand command) {
        dictManagementService.update(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    /**
     * 删除数据字典。
     *
     * @param id 字典ID
     * @return 操作结果
     */
    @PostMapping("/{id}/delete")
    @Operation(summary = "删除数据字典")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dictManagementService.delete(id);
        return ApiResponse.successMessage("删除成功");
    }
}
