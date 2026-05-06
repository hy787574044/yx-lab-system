package com.yx.lab.modules.asset.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.asset.dto.DocumentQuery;
import com.yx.lab.modules.asset.dto.InstrumentQuery;
import com.yx.lab.modules.asset.dto.MaintenanceQuery;
import com.yx.lab.modules.asset.entity.Instrument;
import com.yx.lab.modules.asset.entity.InstrumentMaintenance;
import com.yx.lab.modules.asset.entity.LabDocument;
import com.yx.lab.modules.asset.mapper.InstrumentMaintenanceMapper;
import com.yx.lab.modules.asset.mapper.InstrumentMapper;
import com.yx.lab.modules.asset.mapper.LabDocumentMapper;
import com.yx.lab.modules.asset.vo.InstrumentImportErrorVO;
import com.yx.lab.modules.asset.vo.InstrumentImportResultVO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AssetService {

    private static final List<String> IMPORT_HEADERS = Arrays.asList(
            "设备名称*",
            "设备型号",
            "生产厂家",
            "负责人",
            "设备状态*",
            "存放位置",
            "购置日期(yyyy-MM-dd)",
            "使用年限",
            "校准周期",
            "证书地址",
            "备注");

    private static final String[] ALLOWED_STATUS_ARRAY = new String[]{
            "NORMAL",
            "DISABLED",
            "MAINTENANCE",
            "CALIBRATING"};

    private static final Set<String> ALLOWED_STATUSES = new HashSet<>(Arrays.asList(ALLOWED_STATUS_ARRAY));

    private final InstrumentMapper instrumentMapper;

    private final InstrumentMaintenanceMapper maintenanceMapper;

    private final LabDocumentMapper documentMapper;

    public PageResult<Instrument> instrumentPage(InstrumentQuery query) {
        Page<Instrument> page = instrumentMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<Instrument>()
                        .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                                .like(Instrument::getInstrumentName, query.getKeyword())
                                .or()
                                .like(Instrument::getInstrumentModel, query.getKeyword())
                                .or()
                                .like(Instrument::getManufacturer, query.getKeyword())
                                .or()
                                .like(Instrument::getStorageLocation, query.getKeyword()))
                        .eq(StrUtil.isNotBlank(query.getInstrumentStatus()), Instrument::getInstrumentStatus, query.getInstrumentStatus())
                        .orderByDesc(Instrument::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public Instrument instrumentDetail(Long id) {
        Instrument instrument = instrumentMapper.selectById(id);
        if (instrument == null) {
            throw new BusinessException("设备不存在或已删除");
        }
        return instrument;
    }

    public PageResult<InstrumentMaintenance> maintenancePage(MaintenanceQuery query) {
        Page<InstrumentMaintenance> page = maintenanceMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<InstrumentMaintenance>()
                        .eq(query.getInstrumentId() != null, InstrumentMaintenance::getInstrumentId, query.getInstrumentId())
                        .orderByDesc(InstrumentMaintenance::getMaintenanceTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public PageResult<LabDocument> documentPage(DocumentQuery query) {
        Page<LabDocument> page = documentMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<LabDocument>()
                        .like(StrUtil.isNotBlank(query.getKeyword()), LabDocument::getDocumentName, query.getKeyword())
                        .eq(StrUtil.isNotBlank(query.getDocumentCategory()), LabDocument::getDocumentCategory, query.getDocumentCategory())
                        .orderByDesc(LabDocument::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public void saveInstrument(Instrument entity) {
        if (StrUtil.isBlank(entity.getInstrumentStatus())) {
            entity.setInstrumentStatus("NORMAL");
        }
        instrumentMapper.insert(entity);
    }

    public void updateInstrument(Instrument entity) {
        if (instrumentMapper.selectById(entity.getId()) == null) {
            throw new BusinessException("设备不存在或已删除");
        }
        instrumentMapper.updateById(entity);
    }

    public void deleteInstrument(Long id) {
        if (instrumentMapper.selectById(id) == null) {
            throw new BusinessException("设备不存在或已删除");
        }
        instrumentMapper.deleteById(id);
    }

    public byte[] buildInstrumentImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("设备台账导入模板");
            createHeaderRow(workbook, sheet);
            addStatusValidation(sheet);
            createInstructionSheet(workbook);
            for (int i = 0; i < IMPORT_HEADERS.size(); i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i), 18 * 256));
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException("生成导入模板失败: " + exception.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public InstrumentImportResultVO importInstruments(MultipartFile file) {
        validateImportFile(file);
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            validateHeaders(sheet);
            List<InstrumentImportErrorVO> errors = new ArrayList<>();
            List<Instrument> instruments = new ArrayList<>();
            Set<String> seenKeys = new HashSet<>();
            DataFormatter formatter = new DataFormatter();

            int lastRowNum = sheet.getLastRowNum();
            for (int rowIndex = 1; rowIndex <= lastRowNum; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyRow(row, formatter)) {
                    continue;
                }
                Instrument instrument = parseInstrumentRow(row, formatter, errors, seenKeys);
                if (instrument != null) {
                    instruments.add(instrument);
                }
            }

            InstrumentImportResultVO result = new InstrumentImportResultVO();
            result.setTotalRows(instruments.size() + errors.size());
            result.setErrors(errors);
            result.setAllPassed(errors.isEmpty());

            if (result.getTotalRows() == 0) {
                throw new BusinessException("导入文件中没有可用的数据行");
            }
            if (!errors.isEmpty()) {
                return result;
            }

            for (Instrument instrument : instruments) {
                instrumentMapper.insert(instrument);
            }
            result.setSuccessCount(instruments.size());
            return result;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("导入设备台账失败: " + exception.getMessage());
        }
    }

    public void saveMaintenance(InstrumentMaintenance entity) {
        maintenanceMapper.insert(entity);
    }

    public void updateMaintenance(InstrumentMaintenance entity) {
        maintenanceMapper.updateById(entity);
    }

    public void deleteMaintenance(Long id) {
        maintenanceMapper.deleteById(id);
    }

    public void saveDocument(LabDocument entity) {
        documentMapper.insert(entity);
    }

    public void updateDocument(LabDocument entity) {
        documentMapper.updateById(entity);
    }

    public void deleteDocument(Long id) {
        documentMapper.deleteById(id);
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传导入文件");
        }
        String fileName = file.getOriginalFilename();
        String lowerName = fileName == null ? "" : fileName.toLowerCase();
        if (!lowerName.endsWith(".xlsx") && !lowerName.endsWith(".xls")) {
            throw new BusinessException("仅支持导入 Excel 文件(.xlsx/.xls)");
        }
    }

    private void validateHeaders(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new BusinessException("导入模板表头缺失，请重新下载模板");
        }
        for (int i = 0; i < IMPORT_HEADERS.size(); i++) {
            String actual = getCellString(headerRow.getCell(i), new DataFormatter());
            if (!IMPORT_HEADERS.get(i).equals(actual)) {
                throw new BusinessException("导入模板表头不匹配，请重新下载最新模板");
            }
        }
    }

    private Instrument parseInstrumentRow(Row row, DataFormatter formatter, List<InstrumentImportErrorVO> errors, Set<String> seenKeys) {
        int rowNum = row.getRowNum() + 1;
        String instrumentName = getCellString(row.getCell(0), formatter);
        String instrumentModel = getCellString(row.getCell(1), formatter);
        String manufacturer = getCellString(row.getCell(2), formatter);
        String ownerName = getCellString(row.getCell(3), formatter);
        String instrumentStatus = getCellString(row.getCell(4), formatter);
        String storageLocation = getCellString(row.getCell(5), formatter);
        String purchaseDateText = getCellString(row.getCell(6), formatter);
        String serviceLifeYearsText = getCellString(row.getCell(7), formatter);
        String calibrationCycle = getCellString(row.getCell(8), formatter);
        String certificateUrl = getCellString(row.getCell(9), formatter);
        String remark = getCellString(row.getCell(10), formatter);

        List<String> rowErrors = new ArrayList<>();
        if (StrUtil.isBlank(instrumentName)) {
            rowErrors.add("设备名称不能为空");
        }
        if (StrUtil.isBlank(instrumentStatus)) {
            rowErrors.add("设备状态不能为空");
        } else if (!ALLOWED_STATUSES.contains(instrumentStatus)) {
            rowErrors.add("设备状态仅支持 NORMAL、DISABLED、MAINTENANCE、CALIBRATING");
        }

        Integer serviceLifeYears = null;
        if (StrUtil.isNotBlank(serviceLifeYearsText)) {
            try {
                BigDecimal years = new BigDecimal(serviceLifeYearsText.trim());
                if (years.scale() > 0) {
                    rowErrors.add("使用年限必须为整数");
                } else {
                    serviceLifeYears = years.intValueExact();
                    if (serviceLifeYears < 0) {
                        rowErrors.add("使用年限不能小于 0");
                    }
                }
            } catch (Exception exception) {
                rowErrors.add("使用年限格式不正确");
            }
        }

        LocalDate purchaseDate = null;
        if (StrUtil.isNotBlank(purchaseDateText)) {
            try {
                purchaseDate = LocalDate.parse(purchaseDateText.trim());
            } catch (DateTimeParseException exception) {
                rowErrors.add("购置日期格式应为 yyyy-MM-dd");
            }
        }

        String uniqueKey = buildUniqueKey(instrumentName, instrumentModel, manufacturer);
        if (seenKeys.contains(uniqueKey)) {
            rowErrors.add("导入文件中存在重复设备");
        } else if (existsInstrument(instrumentName, instrumentModel, manufacturer)) {
            rowErrors.add("系统中已存在同名同型号设备");
        } else if (rowErrors.isEmpty()) {
            seenKeys.add(uniqueKey);
        }

        if (!rowErrors.isEmpty()) {
            errors.add(new InstrumentImportErrorVO(rowNum, instrumentName, String.join("；", rowErrors)));
            return null;
        }

        Instrument instrument = new Instrument();
        instrument.setInstrumentName(instrumentName);
        instrument.setInstrumentModel(instrumentModel);
        instrument.setManufacturer(manufacturer);
        instrument.setOwnerName(ownerName);
        instrument.setInstrumentStatus(instrumentStatus);
        instrument.setStorageLocation(storageLocation);
        instrument.setPurchaseDate(purchaseDate);
        instrument.setServiceLifeYears(serviceLifeYears);
        instrument.setCalibrationCycle(calibrationCycle);
        instrument.setCertificateUrl(certificateUrl);
        instrument.setRemark(remark);
        return instrument;
    }

    private boolean existsInstrument(String instrumentName, String instrumentModel, String manufacturer) {
        List<Instrument> instruments = instrumentMapper.selectList(new LambdaQueryWrapper<Instrument>()
                .eq(Instrument::getInstrumentName, instrumentName));
        String normalizedModel = normalizeCompareValue(instrumentModel);
        String normalizedManufacturer = normalizeCompareValue(manufacturer);
        return instruments.stream().anyMatch(item ->
                normalizedModel.equals(normalizeCompareValue(item.getInstrumentModel()))
                        && normalizedManufacturer.equals(normalizeCompareValue(item.getManufacturer())));
    }

    private String buildUniqueKey(String instrumentName, String instrumentModel, String manufacturer) {
        return normalizeCompareValue(instrumentName) + "||"
                + normalizeCompareValue(instrumentModel) + "||"
                + normalizeCompareValue(manufacturer);
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < IMPORT_HEADERS.size(); i++) {
            if (StrUtil.isNotBlank(getCellString(row.getCell(i), formatter))) {
                return false;
            }
        }
        return true;
    }

    private String getCellString(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate().toString();
        }
        return StrUtil.trim(formatter.formatCellValue(cell));
    }

    private void createHeaderRow(Workbook workbook, Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        for (int i = 0; i < IMPORT_HEADERS.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(IMPORT_HEADERS.get(i));
            cell.setCellStyle(headerStyle);
        }
    }

    private void addStatusValidation(Sheet sheet) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(
                ALLOWED_STATUS_ARRAY);
        CellRangeAddressList regions = new CellRangeAddressList(1, 500, 4, 4);
        DataValidation validation = helper.createValidation(constraint, regions);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private String normalizeCompareValue(String value) {
        return StrUtil.blankToDefault(StrUtil.trim(value), "").toLowerCase();
    }

    private void createInstructionSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("填写说明");
        String[] lines = new String[]{
                "1. 仅支持导入新增设备，不会覆盖系统中已有设备。",
                "2. 必填字段：设备名称、设备状态。",
                "3. 设备状态只允许：NORMAL、DISABLED、MAINTENANCE、CALIBRATING。",
                "4. 购置日期格式必须为 yyyy-MM-dd。",
                "5. 使用年限必须为非负整数。",
                "6. 系统会校验导入文件内重复数据，以及系统中已存在的同名同型号同厂家设备。",
                "7. 只要有一行校验失败，本次导入不会入库，请修正后重新导入。"
        };
        for (int i = 0; i < lines.length; i++) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue(lines[i]);
        }
        sheet.setColumnWidth(0, 110 * 256);
    }
}
