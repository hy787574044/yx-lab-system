package com.yx.lab.modules.storage.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.modules.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String filePath = storageService.store(file);
        return ApiResponse.success(Collections.singletonMap("filePath", filePath));
    }
}
