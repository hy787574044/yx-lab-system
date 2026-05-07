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

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final InstrumentAssetService instrumentAssetService;

    private final InstrumentAssetImportService instrumentAssetImportService;

    private final AssetDocumentService assetDocumentService;

    @GetMapping("/instruments")
    public ApiResponse<PageResult<Instrument>> instruments(@Validated InstrumentQuery query) {
        return ApiResponse.success(instrumentAssetService.instrumentPage(query));
    }

    @GetMapping("/instruments/{id}")
    public ApiResponse<Instrument> instrumentDetail(@PathVariable Long id) {
        return ApiResponse.success(instrumentAssetService.instrumentDetail(id));
    }

    @GetMapping("/instruments/import-template")
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

    @PostMapping("/instruments/import")
    public ApiResponse<InstrumentImportResultVO> importInstruments(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(instrumentAssetImportService.importInstruments(file));
    }

    @PostMapping("/instruments")
    public ApiResponse<Void> saveInstrument(@Valid @RequestBody InstrumentSaveCommand command) {
        instrumentAssetService.saveInstrument(command);
        return ApiResponse.successMessage("\u65b0\u589e\u6210\u529f");
    }

    @PutMapping("/instruments/{id}")
    public ApiResponse<Void> updateInstrument(@PathVariable Long id, @Valid @RequestBody InstrumentSaveCommand command) {
        instrumentAssetService.updateInstrument(id, command);
        return ApiResponse.successMessage("\u66f4\u65b0\u6210\u529f");
    }

    @DeleteMapping("/instruments/{id}")
    public ApiResponse<Void> deleteInstrument(@PathVariable Long id) {
        instrumentAssetService.deleteInstrument(id);
        return ApiResponse.successMessage("\u5220\u9664\u6210\u529f");
    }

    @GetMapping("/maintenances")
    public ApiResponse<PageResult<InstrumentMaintenance>> maintenances(@Validated MaintenanceQuery query) {
        return ApiResponse.success(instrumentAssetService.maintenancePage(query));
    }

    @PostMapping("/maintenances")
    public ApiResponse<Void> saveMaintenance(@Valid @RequestBody InstrumentMaintenanceSaveCommand command) {
        instrumentAssetService.saveMaintenance(command);
        return ApiResponse.successMessage("\u65b0\u589e\u6210\u529f");
    }

    @PutMapping("/maintenances/{id}")
    public ApiResponse<Void> updateMaintenance(@PathVariable Long id, @Valid @RequestBody InstrumentMaintenanceSaveCommand command) {
        instrumentAssetService.updateMaintenance(id, command);
        return ApiResponse.successMessage("\u66f4\u65b0\u6210\u529f");
    }

    @DeleteMapping("/maintenances/{id}")
    public ApiResponse<Void> deleteMaintenance(@PathVariable Long id) {
        instrumentAssetService.deleteMaintenance(id);
        return ApiResponse.successMessage("\u5220\u9664\u6210\u529f");
    }

    @GetMapping("/document-users")
    public ApiResponse<List<DocumentUserOptionVO>> documentUsers() {
        return ApiResponse.success(assetDocumentService.documentUserOptions());
    }

    @GetMapping("/documents")
    public ApiResponse<PageResult<LabDocumentVO>> documents(@Validated DocumentQuery query) {
        return ApiResponse.success(assetDocumentService.documentPage(query));
    }

    @GetMapping("/documents/{id}")
    public ApiResponse<LabDocumentVO> documentDetail(@PathVariable Long id) {
        return ApiResponse.success(assetDocumentService.documentDetail(id));
    }

    @GetMapping("/documents/{id}/preview")
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

    @PostMapping("/documents")
    public ApiResponse<Void> saveDocument(@Valid @RequestBody DocumentSaveCommand command) {
        assetDocumentService.saveDocument(command);
        return ApiResponse.successMessage("\u65b0\u589e\u6210\u529f");
    }

    @PutMapping("/documents/{id}")
    public ApiResponse<Void> updateDocument(@PathVariable Long id, @Valid @RequestBody DocumentSaveCommand command) {
        assetDocumentService.updateDocument(id, command);
        return ApiResponse.successMessage("\u66f4\u65b0\u6210\u529f");
    }

    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        assetDocumentService.deleteDocumentWithPermission(id);
        return ApiResponse.successMessage("\u5220\u9664\u6210\u529f");
    }
}
