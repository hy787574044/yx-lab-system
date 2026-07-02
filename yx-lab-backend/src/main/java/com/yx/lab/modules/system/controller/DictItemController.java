package com.yx.lab.modules.system.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.security.PermissionConstants;
import com.yx.lab.common.security.RequirePermission;
import com.yx.lab.modules.system.service.DictManagementService;
import com.yx.lab.modules.system.vo.DictItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据字典项只读接口，供业务页面读取通用下拉选项。
 */
@RestController
@RequestMapping("/api/dicts")
@RequiredArgsConstructor
@Tag(name = "数据字典项")
public class DictItemController {

    private final DictManagementService dictManagementService;

    /**
     * 按字典编码读取启用的字典项。
     *
     * @param dictCode 字典编码
     * @return 字典项列表
     */
    @GetMapping("/{dictCode}/items")
    @Operation(summary = "读取字典项")
    @RequirePermission(
            value = {
                    PermissionConstants.SAMPLE_VIEW,
                    PermissionConstants.SAMPLING_TASK_VIEW,
                    PermissionConstants.SAMPLING_PLAN_VIEW,
                    PermissionConstants.SYSTEM_VIEW
            },
            any = true)
    public ApiResponse<List<DictItemVO>> items(@PathVariable("dictCode") String dictCode) {
        return ApiResponse.success(dictManagementService.items(dictCode));
    }
}
