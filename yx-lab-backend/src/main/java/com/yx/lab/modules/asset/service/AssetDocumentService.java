package com.yx.lab.modules.asset.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.asset.dto.DocumentQuery;
import com.yx.lab.modules.asset.dto.DocumentSaveCommand;
import com.yx.lab.modules.asset.entity.LabDocument;
import com.yx.lab.modules.asset.entity.LabDocumentShare;
import com.yx.lab.modules.asset.mapper.LabDocumentMapper;
import com.yx.lab.modules.asset.mapper.LabDocumentShareMapper;
import com.yx.lab.modules.asset.vo.DocumentPreviewFile;
import com.yx.lab.modules.asset.vo.DocumentUserOptionVO;
import com.yx.lab.modules.asset.vo.LabDocumentVO;
import com.yx.lab.modules.storage.service.StorageService;
import com.yx.lab.modules.system.entity.LabUser;
import com.yx.lab.modules.system.mapper.LabUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetDocumentService {

    private final LabDocumentMapper documentMapper;

    private final LabDocumentShareMapper documentShareMapper;

    private final LabUserMapper labUserMapper;

    private final StorageService storageService;

    public List<DocumentUserOptionVO> documentUserOptions() {
        List<LabUser> users = labUserMapper.selectList(new LambdaQueryWrapper<LabUser>()
                .eq(LabUser::getStatus, 1)
                .orderByAsc(LabUser::getRealName)
                .orderByAsc(LabUser::getUsername));
        return users.stream()
                .map(user -> DocumentUserOptionVO.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .realName(user.getRealName())
                        .roleCode(user.getRoleCode())
                        .build())
                .collect(Collectors.toList());
    }

    public PageResult<LabDocumentVO> documentPage(DocumentQuery query) {
        CurrentUser currentUser = requireCurrentUser();
        Set<Long> sharedDocumentIds = listSharedDocumentIds(currentUser.getUserId());

        LambdaQueryWrapper<LabDocument> wrapper = new LambdaQueryWrapper<LabDocument>()
                .like(StrUtil.isNotBlank(query.getKeyword()), LabDocument::getDocumentName, query.getKeyword())
                .eq(StrUtil.isNotBlank(query.getDocumentCategory()), LabDocument::getDocumentCategory, query.getDocumentCategory())
                .orderByDesc(LabDocument::getCreatedTime);

        if (!isAdmin(currentUser)) {
            wrapper.and(inner -> {
                inner.eq(LabDocument::getCreatedBy, currentUser.getUserId());
                if (!sharedDocumentIds.isEmpty()) {
                    inner.or().in(LabDocument::getId, sharedDocumentIds);
                }
            });
        }

        Page<LabDocument> page = documentMapper.selectPage(PageUtils.buildPage(query), wrapper);
        List<LabDocumentVO> records = enrichDocuments(page.getRecords(), currentUser);
        return new PageResult<>(page.getTotal(), records);
    }

    public LabDocumentVO documentDetail(Long id) {
        CurrentUser currentUser = requireCurrentUser();
        LabDocument document = getAccessibleDocument(id, currentUser);
        return enrichDocument(document, currentUser, listSharesByDocumentIds(Collections.singleton(document.getId())));
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveDocument(DocumentSaveCommand command) {
        LabDocument document = new LabDocument();
        applyDocumentCommand(document, command);
        documentMapper.insert(document);
        replaceDocumentShares(document.getId(), command.getViewerUserIds());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDocument(Long id, DocumentSaveCommand command) {
        CurrentUser currentUser = requireCurrentUser();
        LabDocument existing = getManageableDocument(id, currentUser);
        String oldFileUrl = existing.getFileUrl();
        applyDocumentCommand(existing, command);
        documentMapper.updateById(existing);
        replaceDocumentShares(id, command.getViewerUserIds());
        deleteReplacedDocumentFile(oldFileUrl, existing.getFileUrl());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDocumentWithPermission(Long id) {
        CurrentUser currentUser = requireCurrentUser();
        LabDocument existing = getManageableDocument(id, currentUser);
        documentShareMapper.deleteAllByDocumentId(id);
        documentMapper.deleteById(existing.getId());
        deleteDocumentFileQuietly(existing.getFileUrl());
    }

    public DocumentPreviewFile previewDocument(Long id) {
        CurrentUser currentUser = requireCurrentUser();
        LabDocument document = getAccessibleDocument(id, currentUser);
        try {
            Path path = storageService.resolvePath(document.getFileUrl());
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                throw new BusinessException("Document file not found");
            }
            String contentType = Files.probeContentType(path);
            if (StrUtil.isBlank(contentType)) {
                contentType = detectContentType(document.getFileType(), document.getFileUrl());
            }
            return new DocumentPreviewFile(document.getDocumentName(), contentType, Files.readAllBytes(path));
        } catch (IOException exception) {
            throw new BusinessException("Failed to read document: " + exception.getMessage());
        }
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException("Please login first");
        }
        return currentUser;
    }

    private boolean isAdmin(CurrentUser currentUser) {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRoleCode());
    }

    private LabDocument getAccessibleDocument(Long id, CurrentUser currentUser) {
        LabDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException("Document not found");
        }
        if (isAdmin(currentUser) || Objects.equals(document.getCreatedBy(), currentUser.getUserId())) {
            return document;
        }
        Long count = documentShareMapper.selectCount(new LambdaQueryWrapper<LabDocumentShare>()
                .eq(LabDocumentShare::getDocumentId, id)
                .eq(LabDocumentShare::getUserId, currentUser.getUserId()));
        if (count == null || count == 0L) {
            throw new BusinessException("No permission to view document");
        }
        return document;
    }

    private LabDocument getManageableDocument(Long id, CurrentUser currentUser) {
        LabDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException("Document not found");
        }
        if (isAdmin(currentUser) || Objects.equals(document.getCreatedBy(), currentUser.getUserId())) {
            return document;
        }
        throw new BusinessException("No permission to update document");
    }

    private Set<Long> listSharedDocumentIds(Long userId) {
        List<LabDocumentShare> shares = documentShareMapper.selectList(new LambdaQueryWrapper<LabDocumentShare>()
                .eq(LabDocumentShare::getUserId, userId));
        return shares.stream()
                .map(LabDocumentShare::getDocumentId)
                .collect(Collectors.toSet());
    }

    private Map<Long, List<LabDocumentShare>> listSharesByDocumentIds(Set<Long> documentIds) {
        if (documentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<LabDocumentShare> shares = documentShareMapper.selectList(new LambdaQueryWrapper<LabDocumentShare>()
                .in(LabDocumentShare::getDocumentId, documentIds)
                .orderByAsc(LabDocumentShare::getRealName)
                .orderByAsc(LabDocumentShare::getUsername));
        Map<Long, List<LabDocumentShare>> grouped = new HashMap<>();
        for (LabDocumentShare share : shares) {
            grouped.computeIfAbsent(share.getDocumentId(), key -> new ArrayList<>()).add(share);
        }
        return grouped;
    }

    private List<LabDocumentVO> enrichDocuments(List<LabDocument> documents, CurrentUser currentUser) {
        Set<Long> documentIds = documents.stream().map(LabDocument::getId).collect(Collectors.toSet());
        Map<Long, List<LabDocumentShare>> shareMap = listSharesByDocumentIds(documentIds);
        return documents.stream()
                .map(document -> enrichDocument(document, currentUser, shareMap))
                .collect(Collectors.toList());
    }

    private LabDocumentVO enrichDocument(LabDocument document,
                                         CurrentUser currentUser,
                                         Map<Long, List<LabDocumentShare>> shareMap) {
        boolean canManage = isAdmin(currentUser) || Objects.equals(document.getCreatedBy(), currentUser.getUserId());
        List<LabDocumentShare> shares = shareMap.getOrDefault(document.getId(), Collections.emptyList());
        List<Long> viewerUserIds = shares.stream()
                .map(LabDocumentShare::getUserId)
                .collect(Collectors.toList());
        List<String> viewerNames = shares.stream()
                .map(share -> StrUtil.blankToDefault(share.getRealName(), share.getUsername()))
                .collect(Collectors.toList());
        return LabDocumentVO.builder()
                .id(document.getId())
                .documentName(document.getDocumentName())
                .documentCategory(document.getDocumentCategory())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .fileUrl(canManage ? document.getFileUrl() : null)
                .remark(document.getRemark())
                .createdName(document.getCreatedName())
                .createdTime(document.getCreatedTime())
                .canManage(canManage)
                .viewerUserIds(viewerUserIds)
                .viewerNames(viewerNames)
                .build();
    }

    private void applyDocumentCommand(LabDocument document, DocumentSaveCommand command) {
        document.setDocumentName(StrUtil.trim(command.getDocumentName()));
        document.setDocumentCategory(StrUtil.trim(command.getDocumentCategory()));
        document.setFileType(StrUtil.blankToDefault(StrUtil.trim(command.getFileType()), ""));
        document.setFileSize(command.getFileSize());
        document.setFileUrl(StrUtil.trim(command.getFileUrl()));
        document.setRemark(StrUtil.trim(command.getRemark()));
        if (StrUtil.isBlank(document.getFileType())) {
            document.setFileType(detectFileType(document.getFileUrl()));
        }
        if (StrUtil.isBlank(document.getFileUrl())) {
            throw new BusinessException("Please upload document file first");
        }
    }

    private void replaceDocumentShares(Long documentId, List<String> viewerUserIds) {
        documentShareMapper.deleteAllByDocumentId(documentId);
        List<Long> ids = parseViewerIds(viewerUserIds);
        if (ids.isEmpty()) {
            return;
        }
        List<LabUser> users = labUserMapper.selectBatchIds(ids);
        Map<Long, LabUser> userMap = users.stream()
                .collect(Collectors.toMap(LabUser::getId, item -> item));
        for (Long userId : ids) {
            LabUser user = userMap.get(userId);
            if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                throw new BusinessException("Invalid document viewer");
            }
            LabDocumentShare share = new LabDocumentShare();
            share.setDocumentId(documentId);
            share.setUserId(user.getId());
            share.setUsername(user.getUsername());
            share.setRealName(user.getRealName());
            documentShareMapper.insert(share);
        }
    }

    private List<Long> parseViewerIds(List<String> viewerUserIds) {
        if (viewerUserIds == null || viewerUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> uniqueIds = new LinkedHashSet<>();
        for (String viewerUserId : viewerUserIds) {
            if (StrUtil.isBlank(viewerUserId)) {
                continue;
            }
            try {
                uniqueIds.add(Long.valueOf(viewerUserId));
            } catch (NumberFormatException exception) {
                throw new BusinessException("Invalid viewer user id");
            }
        }
        return new ArrayList<>(uniqueIds);
    }

    private String detectFileType(String fileUrl) {
        String ext = StrUtil.subAfter(fileUrl, ".", true);
        return StrUtil.blankToDefault(ext, "").toLowerCase();
    }

    private String detectContentType(String fileType, String fileUrl) {
        String ext = StrUtil.blankToDefault(StrUtil.trim(fileType), "");
        if (StrUtil.isBlank(ext)) {
            ext = detectFileType(fileUrl);
        }
        switch (ext.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "txt":
                return "text/plain;charset=UTF-8";
            case "html":
            case "htm":
                return "text/html;charset=UTF-8";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default:
                return "application/octet-stream";
        }
    }

    private void deleteReplacedDocumentFile(String oldFileUrl, String newFileUrl) {
        if (StrUtil.isBlank(oldFileUrl) || Objects.equals(oldFileUrl, newFileUrl)) {
            return;
        }
        deleteDocumentFileQuietly(oldFileUrl);
    }

    private void deleteDocumentFileQuietly(String fileUrl) {
        if (StrUtil.isBlank(fileUrl)) {
            return;
        }
        try {
            storageService.deleteIfExists(fileUrl);
        } catch (IOException exception) {
            throw new BusinessException("Failed to delete document file: " + exception.getMessage());
        }
    }
}
