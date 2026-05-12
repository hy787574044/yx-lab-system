package com.yx.lab.modules.asset.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.modules.asset.entity.Instrument;
import com.yx.lab.modules.asset.mapper.InstrumentMapper;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 设备台账导入服务，负责导入模板生成与 Excel 批量导入校验。
 */
@Service
@RequiredArgsConstructor
public class InstrumentAssetImportService {

    private static final List<String> IMPORT_HEADERS = Arrays.asList(
            "\u8bbe\u5907\u540d\u79f0*",
            "\u8bbe\u5907\u578b\u53f7",
            "\u751f\u4ea7\u5382\u5bb6",
            "\u8d1f\u8d23\u4eba",
            "\u8bbe\u5907\u72b6\u6001*",
            "\u5b58\u653e\u4f4d\u7f6e",
            "\u8d2d\u7f6e\u65e5\u671f(yyyy-MM-dd)",
            "\u4f7f\u7528\u5e74\u9650",
            "\u6821\u51c6\u5468\u671f",
            "\u8bc1\u4e66\u5730\u5740",
            "\u5907\u6ce8");

    private static final Map<String, String> INSTRUMENT_STATUS_LABEL_MAP = createInstrumentStatusLabelMap();

    private static final String[] IMPORT_STATUS_OPTIONS = INSTRUMENT_STATUS_LABEL_MAP.values().toArray(new String[0]);

    private static final Set<String> ALLOWED_STATUSES = new HashSet<>(LabWorkflowConstants.INSTRUMENT_STATUSES);

    private static final Map<String, String> IMPORT_STATUS_ALIAS_MAP = createImportStatusAliasMap();

    private final InstrumentMapper instrumentMapper;

    /**
     * 生成设备台账导入模板。
     *
     * @return 模板文件字节数组
     */
    public byte[] buildInstrumentImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("\u8bbe\u5907\u53f0\u8d26\u5bfc\u5165\u6a21\u677f");
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
            throw new BusinessException("Failed to build instrument import template: " + exception.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 导入设备台账 Excel，并返回校验及入库结果。
     *
     * @param file 导入文件
     * @return 导入结果
     */
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
                throw new BusinessException("No valid data rows found in import file");
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
            throw new BusinessException("Failed to import instruments: " + exception.getMessage());
        }
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Please upload an Excel file");
        }
        String fileName = file.getOriginalFilename();
        String lowerName = fileName == null ? "" : fileName.toLowerCase();
        if (!lowerName.endsWith(".xlsx") && !lowerName.endsWith(".xls")) {
            throw new BusinessException("Only .xlsx or .xls files are supported");
        }
    }

    private void validateHeaders(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new BusinessException("Import template header is missing");
        }
        for (int i = 0; i < IMPORT_HEADERS.size(); i++) {
            String actual = getCellString(headerRow.getCell(i), new DataFormatter());
            if (!IMPORT_HEADERS.get(i).equals(actual)) {
                throw new BusinessException("Import template header does not match");
            }
        }
    }

    private Instrument parseInstrumentRow(Row row,
                                          DataFormatter formatter,
                                          List<InstrumentImportErrorVO> errors,
                                          Set<String> seenKeys) {
        int rowNum = row.getRowNum() + 1;
        String instrumentName = getCellString(row.getCell(0), formatter);
        String instrumentModel = getCellString(row.getCell(1), formatter);
        String manufacturer = getCellString(row.getCell(2), formatter);
        String ownerName = getCellString(row.getCell(3), formatter);
        String rawInstrumentStatus = getCellString(row.getCell(4), formatter);
        String instrumentStatus = normalizeInstrumentStatus(rawInstrumentStatus);
        String storageLocation = getCellString(row.getCell(5), formatter);
        String purchaseDateText = getCellString(row.getCell(6), formatter);
        String serviceLifeYearsText = getCellString(row.getCell(7), formatter);
        String calibrationCycle = getCellString(row.getCell(8), formatter);
        String certificateUrl = getCellString(row.getCell(9), formatter);
        String remark = getCellString(row.getCell(10), formatter);

        List<String> rowErrors = new ArrayList<>();
        if (StrUtil.isBlank(instrumentName)) {
            rowErrors.add("Device name is required");
        }
        if (StrUtil.isBlank(rawInstrumentStatus)) {
            rowErrors.add("Device status is required");
        } else if (!ALLOWED_STATUSES.contains(instrumentStatus)) {
            rowErrors.add("Device status must be one of: 正常, 停用, 维护中, 待校准");
        }

        Integer serviceLifeYears = null;
        if (StrUtil.isNotBlank(serviceLifeYearsText)) {
            try {
                BigDecimal years = new BigDecimal(serviceLifeYearsText.trim());
                if (years.scale() > 0) {
                    rowErrors.add("Service life must be an integer");
                } else {
                    serviceLifeYears = years.intValueExact();
                    if (serviceLifeYears < 0) {
                        rowErrors.add("Service life cannot be negative");
                    }
                }
            } catch (Exception exception) {
                rowErrors.add("Service life format is invalid");
            }
        }

        LocalDate purchaseDate = null;
        if (StrUtil.isNotBlank(purchaseDateText)) {
            try {
                purchaseDate = LocalDate.parse(purchaseDateText.trim());
            } catch (DateTimeParseException exception) {
                rowErrors.add("Purchase date must use yyyy-MM-dd");
            }
        }

        String uniqueKey = buildUniqueKey(instrumentName, instrumentModel, manufacturer);
        if (seenKeys.contains(uniqueKey)) {
            rowErrors.add("Duplicate device found in import file");
        } else if (existsInstrument(instrumentName, instrumentModel, manufacturer)) {
            rowErrors.add("Device already exists in system");
        } else if (rowErrors.isEmpty()) {
            seenKeys.add(uniqueKey);
        }

        if (!rowErrors.isEmpty()) {
            errors.add(new InstrumentImportErrorVO(rowNum, instrumentName, String.join("; ", rowErrors)));
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
        DataValidationConstraint constraint = helper.createExplicitListConstraint(IMPORT_STATUS_OPTIONS);
        CellRangeAddressList regions = new CellRangeAddressList(1, 500, 4, 4);
        DataValidation validation = helper.createValidation(constraint, regions);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private void createInstructionSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("\u586b\u5199\u8bf4\u660e");
        String[] lines = new String[]{
                "1. \u4ec5\u652f\u6301\u5bfc\u5165\u65b0\u589e\u8bbe\u5907\uff0c\u4e0d\u4f1a\u8986\u76d6\u7cfb\u7edf\u4e2d\u5df2\u6709\u8bbe\u5907\u3002",
                "2. \u5fc5\u586b\u5b57\u6bb5\uff1a\u8bbe\u5907\u540d\u79f0\u3001\u8bbe\u5907\u72b6\u6001\u3002",
                "3. \u8bbe\u5907\u72b6\u6001\u53ea\u5141\u8bb8\uff1a\u6b63\u5e38\u3001\u505c\u7528\u3001\u7ef4\u62a4\u4e2d\u3001\u5f85\u6821\u51c6\u3002",
                "4. \u8d2d\u7f6e\u65e5\u671f\u683c\u5f0f\u5fc5\u987b\u4e3a yyyy-MM-dd\u3002",
                "5. \u4f7f\u7528\u5e74\u9650\u5fc5\u987b\u4e3a\u975e\u8d1f\u6574\u6570\u3002",
                "6. \u7cfb\u7edf\u4f1a\u6821\u9a8c\u5bfc\u5165\u6587\u4ef6\u5185\u91cd\u590d\u6570\u636e\uff0c\u4ee5\u53ca\u7cfb\u7edf\u4e2d\u5df2\u5b58\u5728\u7684\u540c\u540d\u540c\u578b\u53f7\u540c\u5382\u5bb6\u8bbe\u5907\u3002",
                "7. \u53ea\u8981\u6709\u4e00\u884c\u6821\u9a8c\u5931\u8d25\uff0c\u672c\u6b21\u5bfc\u5165\u4e0d\u4f1a\u5165\u5e93\uff0c\u8bf7\u4fee\u6b63\u540e\u91cd\u65b0\u5bfc\u5165\u3002"
        };
        for (int i = 0; i < lines.length; i++) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue(lines[i]);
        }
        sheet.setColumnWidth(0, 110 * 256);
    }

    private String normalizeInstrumentStatus(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        return IMPORT_STATUS_ALIAS_MAP.getOrDefault(normalizeCompareValue(value), "");
    }

    private String normalizeCompareValue(String value) {
        return StrUtil.blankToDefault(StrUtil.trim(value), "").toLowerCase();
    }

    private static Map<String, String> createInstrumentStatusLabelMap() {
        Map<String, String> statusMap = new LinkedHashMap<>();
        statusMap.put(LabWorkflowConstants.InstrumentStatus.NORMAL, "\u6b63\u5e38");
        statusMap.put(LabWorkflowConstants.InstrumentStatus.DISABLED, "\u505c\u7528");
        statusMap.put(LabWorkflowConstants.InstrumentStatus.MAINTENANCE, "\u7ef4\u62a4\u4e2d");
        statusMap.put(LabWorkflowConstants.InstrumentStatus.CALIBRATING, "\u5f85\u6821\u51c6");
        return statusMap;
    }

    private static Map<String, String> createImportStatusAliasMap() {
        Map<String, String> aliasMap = new HashMap<>();
        for (Map.Entry<String, String> entry : INSTRUMENT_STATUS_LABEL_MAP.entrySet()) {
            aliasMap.put(entry.getKey().toLowerCase(), entry.getKey());
            aliasMap.put(normalizeStaticValue(entry.getValue()), entry.getKey());
        }
        return aliasMap;
    }

    private static String normalizeStaticValue(String value) {
        return StrUtil.blankToDefault(StrUtil.trim(value), "").toLowerCase();
    }
}
