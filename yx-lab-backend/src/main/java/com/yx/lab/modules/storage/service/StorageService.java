package com.yx.lab.modules.storage.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.yx.lab.common.config.LabStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final LabStorageProperties storageProperties;

    public String store(MultipartFile file) throws IOException {
        String uploadDir = storageProperties.getUploadDir();
        FileUtil.mkdir(uploadDir);
        String ext = FileUtil.extName(file.getOriginalFilename());
        String fileName = IdUtil.fastSimpleUUID() + (ext.isEmpty() ? "" : "." + ext);
        File target = FileUtil.file(uploadDir, fileName);
        file.transferTo(target);
        return target.getPath().replace("\\", "/");
    }
}
