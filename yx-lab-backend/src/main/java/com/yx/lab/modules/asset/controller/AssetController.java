package com.yx.lab.modules.asset.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.modules.asset.dto.DocumentQuery;
import com.yx.lab.modules.asset.dto.DocumentSaveCommand;
import com.yx.lab.modules.asset.dto.InstrumentMaintenanceSaveCommand;
import com.yx.lab.modules.asset.dto.InstrumentQuery;
import com.yx.lab.modules.asset.dto.InstrumentSaveCommand;
import com.yx.lab.modules.asset.dto.MaintenanceQuery;
import com.yx.lab.modules.asset.entity.Instrument;
import com.yx.lab.modules.asset.entity.InstrumentMaintenance;
import com.yx.lab.modules.asset.service.AssetDocumentService;
import com.yx.lab.modules.asset.service.InstrumentAssetImportService;
import com.yx.lab.modules.asset.service.InstrumentAssetService;
import com.yx.lab.modules.asset.vo.DocumentPreviewFile;
import com.yx.lab.modules.asset.vo.DocumentUserOptionVO;
import com.yx.lab.modules.asset.vo.InstrumentImportResultVO;
import com.yx.lab.modules.asset.vo.LabDocumentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 资产与文档控制器。
 * 负责仪器台账、维保记录与化验室文档的查询和维护。
 */
@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Tag(name = "资产与文档管理")
public class AssetController {

    private final InstrumentAssetService instrumentAssetService;

    private final InstrumentAssetImportService instrumentAssetImportService;

    private final AssetDocumentService assetDocumentService;

    /**
     * 分页查询仪器台账。
     *
     * @param query 仪器查询条件。
     * @return 仪器台账分页结果。
     */
    @GetMapping("/instruments")
    @Operation(summary = "仪器台账分页")
    public ApiResponse<PageResult<Instrument>> instruments(@Validated InstrumentQuery query) {
        return ApiResponse.success(instrumentAssetService.instrumentPage(query));
    }

    /**
     * 获取仪器台账详情。
     *
     * @param id 仪器主键。
     * @return 仪器详情。
     */
    @GetMapping("/instruments/{id}")
    @Operation(summary = "仪器台账详情")
    public ApiResponse<Instrument> instrumentDetail(@PathVariable Long id) {
        return ApiResponse.success(instrumentAssetService.instrumentDetail(id));
    }

    /**
     * 下载仪器导入模板。
     *
     * @return 模板文件流。
     */
    @GetMapping("/instruments/import-template")
    @Operation(summary = "下载仪器导入模板")
    public ResponseEntity<byte[]> downloadInstrumentImportTemplate() {
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename("\u8bbe\u5907\u53f0\u8d26\u5bfc\u5165\u6a21\u677f.xlsx", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(instrumentAssetImportService.buildInstrumentImportTemplate());
    }

    /**
     * 导入仪器台账。
     *
     * @param file 导入文件。
     * @return 导入结果。
     */
    @PostMapping("/instruments/import")
    @Operation(summary = "导入仪器台账")
    public ApiResponse<InstrumentImportResultVO> importInstruments(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(instrumentAssetImportService.importInstruments(file));
    }

    /**
     * 新增仪器台账。
     *
     * @param command 仪器保存命令。
     * @return 保存结果。
     */
    @PostMapping("/instruments")
    @Operation(summary = "新增仪器台账")
    public ApiResponse<Void> saveInstrument(@Valid @RequestBody InstrumentSaveCommand command) {
        instrumentAssetService.saveInstrument(command);
        return ApiResponse.successMessage("\u65b0\u589e\u6210\u529f");
    }

    /**
     * 更新仪器台账。
     *
     * @param id 仪器主键。
     * @param command 仪器保存命令。
     * @return 更新结果。
     */
    @PutMapping("/instruments/{id}")
    @Operation(summary = "更新仪器台账")
    public ApiResponse<Void> updateInstrument(@PathVariable Long id, @Valid @RequestBody InstrumentSaveCommand command) {
        instrumentAssetService.updateInstrument(id, command);
        return ApiResponse.successMessage("\u66f4\u65b0\u6210\u529f");
    }

    /**
     * 删除仪器台账。
     *
     * @param id 仪器主键。
     * @return 删除结果。
     */
    @DeleteMapping("/instruments/{id}")
    @Operation(summary = "删除仪器台账")
    public ApiResponse<Void> deleteInstrument(@PathVariable Long id) {
        instrumentAssetService.deleteInstrument(id);
        return ApiResponse.successMessage("\u5220\u9664\u6210\u529f");
    }

    /**
     * 分页查询维保记录。
     *
     * @param query 维保查询条件。
     * @return 维保分页结果。
     */
    @GetMapping("/maintenances")
    @Operation(summary = "维保记录分页")
    public ApiResponse<PageResult<InstrumentMaintenance>> maintenances(@Validated MaintenanceQuery query) {
        return ApiResponse.success(instrumentAssetService.maintenancePage(query));
    }

    /**
     * 新增维保记录。
     *
     * @param command 维保保存命令。
     * @return 保存结果。
     */
    @PostMapping("/maintenances")
    @Operation(summary = "新增维保记录")
    public ApiResponse<Void> saveMaintenance(@Valid @RequestBody InstrumentMaintenanceSaveCommand command) {
        instrumentAssetService.saveMaintenance(command);
        return ApiResponse.successMessage("\u65b0\u589e\u6210\u529f");
    }

    /**
     * 更新维保记录。
     *
     * @param id 维保主键。
     * @param command 维保保存命令。
     * @return 更新结果。
     */
    @PutMapping("/maintenances/{id}")
    @Operation(summary = "更新维保记录")
    public ApiResponse<Void> updateMaintenance(@PathVariable Long id, @Valid @RequestBody InstrumentMaintenanceSaveCommand command) {
        instrumentAssetService.updateMaintenance(id, command);
        return ApiResponse.successMessage("\u66f4\u65b0\u6210\u529f");
    }

    /**
     * 删除维保记录。
     *
     * @param id 维保主键。
     * @return 删除结果。
     */
    @DeleteMapping("/maintenances/{id}")
    @Operation(summary = "删除维保记录")
    public ApiResponse<Void> deleteMaintenance(@PathVariable Long id) {
        instrumentAssetService.deleteMaintenance(id);
        return ApiResponse.successMessage("\u5220\u9664\u6210\u529f");
    }

    /**
     * 获取文档可见人员选项。
     *
     * @return 可见人员列表。
     */
    @GetMapping("/document-users")
    @Operation(summary = "文档可见人员选项")
    public ApiResponse<List<DocumentUserOptionVO>> documentUsers() {
        return ApiResponse.success(assetDocumentService.documentUserOptions());
    }

    /**
     * 分页查询化验室文档。
     *
     * @param query 文档查询条件。
     * @return 文档分页结果。
     */
    @GetMapping("/documents")
    @Operation(summary = "化验室文档分页")
    public ApiResponse<PageResult<LabDocumentVO>> documents(@Validated DocumentQuery query) {
        return ApiResponse.success(assetDocumentService.documentPage(query));
    }

    /**
     * 获取化验室文档详情。
     *
     * @param id 文档主键。
     * @return 文档详情。
     */
    @GetMapping("/documents/{id}")
    @Operation(summary = "化验室文档详情")
    public ApiResponse<LabDocumentVO> documentDetail(@PathVariable Long id) {
        return ApiResponse.success(assetDocumentService.documentDetail(id));
    }

    /**
     * 预览化验室文档。
     *
     * @param id 文档主键。
     * @return 文档文件流。
     */
    @GetMapping("/documents/{id}/preview")
    @Operation(summary = "预览化验室文档")
    public ResponseEntity<byte[]> previewDocument(@PathVariable Long id) {
        DocumentPreviewFile previewFile = assetDocumentService.previewDocument(id);
        ContentDisposition contentDisposition = ContentDisposition.inline()
                .filename(previewFile.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType(previewFile.getContentType()))
                .body(previewFile.getContent());
    }

    /**
     * 新增化验室文档。
     *
     * @param command 文档保存命令。
     * @return 保存结果。
     */
    @PostMapping("/documents")
    @Operation(summary = "新增化验室文档")
    public ApiResponse<Void> saveDocument(@Valid @RequestBody DocumentSaveCommand command) {
        assetDocumentService.saveDocument(command);
        return ApiResponse.successMessage("\u65b0\u589e\u6210\u529f");
    }

    /**
     * 更新化验室文档。
     *
     * @param id 文档主键。
     * @param command 文档保存命令。
     * @return 更新结果。
     */
    @PutMapping("/documents/{id}")
    @Operation(summary = "更新化验室文档")
    public ApiResponse<Void> updateDocument(@PathVariable Long id, @Valid @RequestBody DocumentSaveCommand command) {
        assetDocumentService.updateDocument(id, command);
        return ApiResponse.successMessage("\u66f4\u65b0\u6210\u529f");
    }

    /**
     * 删除化验室文档。
     *
     * @param id 文档主键。
     * @return 删除结果。
     */
    @DeleteMapping("/documents/{id}")
    @Operation(summary = "删除化验室文档")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        assetDocumentService.deleteDocumentWithPermission(id);
        return ApiResponse.successMessage("\u5220\u9664\u6210\u529f");
    }
}
