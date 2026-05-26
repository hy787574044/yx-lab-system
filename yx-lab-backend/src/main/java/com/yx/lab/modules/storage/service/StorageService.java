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
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * File storage service.
 */
@Service
@RequiredArgsConstructor
public class StorageService {

    private final LabStorageProperties storageProperties;

    public String store(MultipartFile file) throws IOException {
        Path uploadRoot = resolveUploadRoot();
        Files.createDirectories(uploadRoot);
        String ext = FileUtil.extName(file.getOriginalFilename());
        String fileName = IdUtil.fastSimpleUUID() + (ext.isEmpty() ? "" : "." + ext);
        File target = uploadRoot.resolve(fileName).toFile();
        file.transferTo(target);
        return fileName;
    }

    public String toFullUrl(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return null;
        }
        String normalizedInput = StrUtil.trim(filePath);
        if (StrUtil.startWithIgnoreCase(normalizedInput, "blob:")
                || StrUtil.startWithIgnoreCase(normalizedInput, "data:")) {
            return normalizedInput;
        }

        String storagePath = extractStoragePath(normalizedInput);
        if (StrUtil.isBlank(storagePath) && isHttpUrl(normalizedInput)) {
            return normalizedInput;
        }

        String fileReference = normalizeFileReference(StrUtil.blankToDefault(storagePath, normalizedInput));
        if (StrUtil.isBlank(fileReference)) {
            return null;
        }

        String requestPath = "api/storage/file?path=" + encodeQueryValue(fileReference);
        String baseUrl = normalizeBaseUrl();
        return StrUtil.isBlank(baseUrl) ? "/" + requestPath : baseUrl + requestPath;
    }

    public String toFullUrls(String filePaths) {
        if (StrUtil.isBlank(filePaths)) {
            return null;
        }
        return Arrays.stream(filePaths.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(this::toFullUrl)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(","));
    }

    public boolean isSameStorageFile(String firstPath, String secondPath) {
        String firstReference = resolveFileReference(firstPath);
        String secondReference = resolveFileReference(secondPath);
        return Objects.equals(firstReference, secondReference);
    }

    public Path resolvePath(String filePath) {
        String fileReference = resolveFileReference(filePath);
        if (StrUtil.isBlank(fileReference)) {
            return resolveUploadRoot();
        }
        if (isHttpUrl(fileReference)) {
            throw new IllegalArgumentException("Unsupported external file url: " + fileReference);
        }
        if (FileUtil.isAbsolutePath(fileReference)) {
            return FileUtil.file(fileReference).toPath().normalize();
        }
        Path normalizedPath = Paths.get(fileReference).normalize();
        Path configuredPath = Paths.get(getConfiguredUploadDir()).normalize();
        if (!configuredPath.isAbsolute() && normalizedPath.startsWith(configuredPath)) {
            return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize()
                    .resolve(normalizedPath)
                    .normalize();
        }
        return resolveUploadRoot().resolve(normalizedPath).normalize();
    }

    public void deleteIfExists(String filePath) throws IOException {
        if (StrUtil.isBlank(filePath)) {
            return;
        }
        Files.deleteIfExists(resolvePath(filePath));
    }

    public String storeText(String relativePath, String content) throws IOException {
        Path targetPath = resolveWritePath(relativePath);
        Files.createDirectories(targetPath.getParent());
        Files.write(targetPath, StrUtil.blankToDefault(content, "").getBytes(StandardCharsets.UTF_8));
        return normalizeRelativePath(relativePath);
    }

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

    private String normalizeBaseUrl() {
        String baseUrl = StrUtil.blankToDefault(storageProperties.getFileBaseUrl(), "").trim();
        if (baseUrl.isEmpty()) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    private Path resolveWritePath(String relativePath) {
        String normalizedPath = normalizeRelativePath(relativePath);
        return resolveUploadRoot().resolve(normalizedPath).normalize();
    }

    private String normalizeRelativePath(String relativePath) {
        return StrUtil.blankToDefault(StrUtil.trim(relativePath), IdUtil.fastSimpleUUID()).replace("\\", "/");
    }

    private String resolveFileReference(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return null;
        }
        String normalizedInput = StrUtil.trim(filePath);
        String storagePath = extractStoragePath(normalizedInput);
        return normalizeFileReference(StrUtil.blankToDefault(storagePath, normalizedInput));
    }

    private String normalizeFileReference(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return null;
        }
        String normalizedPath = StrUtil.trim(filePath).replace("\\", "/");
        if (!FileUtil.isAbsolutePath(normalizedPath)) {
            while (normalizedPath.startsWith("/")) {
                normalizedPath = normalizedPath.substring(1);
            }
        }
        return StrUtil.blankToDefault(normalizedPath, null);
    }

    private String extractStoragePath(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String normalizedValue = StrUtil.trim(value);
        String requestPath = normalizedValue;
        String rawQuery = null;
        if (isHttpUrl(normalizedValue)) {
            try {
                URI uri = URI.create(normalizedValue);
                requestPath = uri.getPath();
                rawQuery = uri.getRawQuery();
            } catch (IllegalArgumentException exception) {
                return null;
            }
        } else {
            int queryIndex = normalizedValue.indexOf('?');
            if (queryIndex >= 0) {
                requestPath = normalizedValue.substring(0, queryIndex);
                rawQuery = normalizedValue.substring(queryIndex + 1);
            }
        }
        requestPath = StrUtil.blankToDefault(StrUtil.trim(requestPath), "");
        while (requestPath.startsWith("/")) {
            requestPath = requestPath.substring(1);
        }
        if (!"api/storage/file".equals(requestPath)) {
            return null;
        }
        return extractQueryParam(rawQuery, "path");
    }

    private String extractQueryParam(String rawQuery, String key) {
        if (StrUtil.isBlank(rawQuery)) {
            return null;
        }
        return Arrays.stream(rawQuery.split("&"))
                .map(item -> item.split("=", 2))
                .filter(parts -> parts.length == 2 && key.equals(decodeQueryValue(parts[0])))
                .map(parts -> decodeQueryValue(parts[1]))
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(null);
    }

    private boolean isHttpUrl(String value) {
        return StrUtil.startWithIgnoreCase(value, "http://")
                || StrUtil.startWithIgnoreCase(value, "https://");
    }

    private String encodeQueryValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (Exception exception) {
            return value;
        }
    }

    private String decodeQueryValue(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (Exception exception) {
            return value;
        }
    }
}
