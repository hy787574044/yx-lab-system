package com.yx.lab.modules.asset.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.modules.asset.dto.DocumentQuery;
import com.yx.lab.modules.asset.dto.DocumentSaveCommand;
import com.yx.lab.modules.asset.dto.InstrumentQuery;
import com.yx.lab.modules.asset.dto.MaintenanceQuery;
import com.yx.lab.modules.asset.entity.Instrument;
import com.yx.lab.modules.asset.entity.InstrumentMaintenance;
import com.yx.lab.modules.asset.service.AssetService;
import com.yx.lab.modules.asset.service.AssetService.PreviewFile;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping("/instruments")
    public ApiResponse<PageResult<Instrument>> instruments(@Validated InstrumentQuery query) {
        return ApiResponse.success(assetService.instrumentPage(query));
    }

    @GetMapping("/instruments/{id}")
    public ApiResponse<Instrument> instrumentDetail(@PathVariable Long id) {
        return ApiResponse.success(assetService.instrumentDetail(id));
    }

    @GetMapping("/instruments/import-template")
    public ResponseEntity<byte[]> downloadInstrumentImportTemplate() {
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename("设备台账导入模板.xlsx", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(assetService.buildInstrumentImportTemplate());
    }

    @PostMapping("/instruments/import")
    public ApiResponse<InstrumentImportResultVO> importInstruments(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(assetService.importInstruments(file));
    }

    @PostMapping("/instruments")
    public ApiResponse<Void> saveInstrument(@Valid @RequestBody Instrument entity) {
        assetService.saveInstrument(entity);
        return ApiResponse.successMessage("新增成功");
    }

    @PutMapping("/instruments/{id}")
    public ApiResponse<Void> updateInstrument(@PathVariable Long id, @Valid @RequestBody Instrument entity) {
        entity.setId(id);
        assetService.updateInstrument(entity);
        return ApiResponse.successMessage("更新成功");
    }

    @DeleteMapping("/instruments/{id}")
    public ApiResponse<Void> deleteInstrument(@PathVariable Long id) {
        assetService.deleteInstrument(id);
        return ApiResponse.successMessage("删除成功");
    }

    @GetMapping("/maintenances")
    public ApiResponse<PageResult<InstrumentMaintenance>> maintenances(@Validated MaintenanceQuery query) {
        return ApiResponse.success(assetService.maintenancePage(query));
    }

    @PostMapping("/maintenances")
    public ApiResponse<Void> saveMaintenance(@RequestBody InstrumentMaintenance entity) {
        assetService.saveMaintenance(entity);
        return ApiResponse.successMessage("新增成功");
    }

    @PutMapping("/maintenances/{id}")
    public ApiResponse<Void> updateMaintenance(@PathVariable Long id, @RequestBody InstrumentMaintenance entity) {
        entity.setId(id);
        assetService.updateMaintenance(entity);
        return ApiResponse.successMessage("更新成功");
    }

    @DeleteMapping("/maintenances/{id}")
    public ApiResponse<Void> deleteMaintenance(@PathVariable Long id) {
        assetService.deleteMaintenance(id);
        return ApiResponse.successMessage("删除成功");
    }

    @GetMapping("/document-users")
    public ApiResponse<java.util.List<DocumentUserOptionVO>> documentUsers() {
        return ApiResponse.success(assetService.documentUserOptions());
    }

    @GetMapping("/documents")
    public ApiResponse<PageResult<LabDocumentVO>> documents(@Validated DocumentQuery query) {
        return ApiResponse.success(assetService.documentPage(query));
    }

    @GetMapping("/documents/{id}")
    public ApiResponse<LabDocumentVO> documentDetail(@PathVariable Long id) {
        return ApiResponse.success(assetService.documentDetail(id));
    }

    @GetMapping("/documents/{id}/preview")
    public ResponseEntity<byte[]> previewDocument(@PathVariable Long id) {
        PreviewFile previewFile = assetService.previewDocument(id);
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
        assetService.saveDocument(command);
        return ApiResponse.successMessage("新增成功");
    }

    @PutMapping("/documents/{id}")
    public ApiResponse<Void> updateDocument(@PathVariable Long id, @Valid @RequestBody DocumentSaveCommand command) {
        assetService.updateDocument(id, command);
        return ApiResponse.successMessage("更新成功");
    }

    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        assetService.deleteDocumentWithPermission(id);
        return ApiResponse.successMessage("删除成功");
    }
}
