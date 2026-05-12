package com.yx.lab.modules.storage.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.yx.lab.common.config.LabStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件存储服务，统一处理上传目录定位、文件读写与删除。
 */
@Service
@RequiredArgsConstructor
public class StorageService {

    private final LabStorageProperties storageProperties;

    /**
     * 保存上传文件并返回相对存储路径。
     *
     * @param file 上传文件
     * @return 存储后的相对路径
     * @throws IOException 文件写入异常
     */
    public String store(MultipartFile file) throws IOException {
        Path uploadRoot = resolveUploadRoot();
        Files.createDirectories(uploadRoot);
        String ext = FileUtil.extName(file.getOriginalFilename());
        String fileName = IdUtil.fastSimpleUUID() + (ext.isEmpty() ? "" : "." + ext);
        File target = uploadRoot.resolve(fileName).toFile();
        file.transferTo(target);
        return fileName;
    }

    /**
     * 解析业务文件路径为本地绝对路径。
     *
     * @param filePath 业务侧保存的文件路径
     * @return 本地绝对路径
     */
    public Path resolvePath(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return resolveUploadRoot();
        }
        if (FileUtil.isAbsolutePath(filePath)) {
            return FileUtil.file(filePath).toPath().normalize();
        }
        Path normalizedPath = Paths.get(filePath).normalize();
        Path configuredPath = Paths.get(getConfiguredUploadDir()).normalize();
        if (!configuredPath.isAbsolute() && normalizedPath.startsWith(configuredPath)) {
            return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize()
                    .resolve(normalizedPath)
                    .normalize();
        }
        return resolveUploadRoot().resolve(normalizedPath).normalize();
    }

    /**
     * 按路径删除文件，文件不存在时直接忽略。
     *
     * @param filePath 业务侧保存的文件路径
     * @throws IOException 文件删除异常
     */
    public void deleteIfExists(String filePath) throws IOException {
        if (StrUtil.isBlank(filePath)) {
            return;
        }
        Files.deleteIfExists(resolvePath(filePath));
    }

    /**
     * 将文本内容按相对路径写入存储目录。
     *
     * @param relativePath 相对存储路径
     * @param content 文本内容
     * @return 规范化后的相对路径
     * @throws IOException 文件写入异常
     */
    public String storeText(String relativePath, String content) throws IOException {
        Path targetPath = resolveWritePath(relativePath);
        Files.createDirectories(targetPath.getParent());
        Files.write(targetPath, StrUtil.blankToDefault(content, "").getBytes(StandardCharsets.UTF_8));
        return normalizeRelativePath(relativePath);
    }

    /**
     * 读取指定文件的完整字节数组。
     *
     * @param filePath 业务侧保存的文件路径
     * @return 文件字节内容
     * @throws IOException 文件读取异常
     */
    public byte[] readAllBytes(String filePath) throws IOException {
        return Files.readAllBytes(resolvePath(filePath));
    }

    private Path resolveUploadRoot() {
        Path configuredPath = Paths.get(getConfiguredUploadDir());
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }
        return Paths.get(System.getProperty("user.dir"))
                .toAbsolutePath()
                .normalize()
                .resolve(configuredPath)
                .normalize();
    }

    private String getConfiguredUploadDir() {
        return StrUtil.blankToDefault(StrUtil.trim(storageProperties.getUploadDir()), "uploads");
    }

    private Path resolveWritePath(String relativePath) {
        String normalizedPath = normalizeRelativePath(relativePath);
        return resolveUploadRoot().resolve(normalizedPath).normalize();
    }

    private String normalizeRelativePath(String relativePath) {
        return StrUtil.blankToDefault(StrUtil.trim(relativePath), IdUtil.fastSimpleUUID()).replace("\\", "/");
    }
}
