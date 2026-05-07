package com.yx.lab.modules.storage.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.modules.storage.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * 文件存储控制器。
 * 负责统一附件上传。
 */
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@Tag(name = "文件存储")
public class StorageController {

    private final StorageService storageService;

    /**
     * 上传文件到统一存储。
     *
     * @param file 上传文件。
     * @return 上传后的文件路径。
     * @throws IOException 文件写入异常。
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String filePath = storageService.store(file);
        return ApiResponse.success(Collections.singletonMap("filePath", filePath));
    }
}
