package com.yx.lab.common.util;

import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.model.PageQuery;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Excel 导出工具。
 */
public final class ExcelExportUtil {

    private static final long EXPORT_PAGE_SIZE = 10_000L;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ExcelExportUtil() {
    }

    /**
     * 将分页查询参数调整为导出查询参数。
     *
     * @param query 分页查询参数
     */
    public static void prepareExportQuery(PageQuery query) {
        if (query == null) {
            return;
        }
        query.setPageNum(1L);
        query.setPageSize(EXPORT_PAGE_SIZE);
    }

    /**
     * 创建导出列定义。
     *
     * @param header 表头
     * @param getter 取值函数
     * @param <T>    数据类型
     * @return 列定义
     */
    public static <T> ExcelColumn<T> column(String header, Function<T, Object> getter) {
        return new ExcelColumn<>(header, getter);
    }

    /**
     * 构建 Excel 下载响应。
     *
     * @param fileName 文件名
     * @param sheetName 工作表名称
     * @param data 数据列表
     * @param columns 列定义
     * @param <T> 数据类型
     * @return 文件响应
     */
    public static <T> ResponseEntity<byte[]> buildResponse(String fileName,
                                                           String sheetName,
                                                           List<T> data,
                                                           List<ExcelColumn<T>> columns) {
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(buildWorkbookBytes(sheetName, data, columns));
    }

    private static <T> byte[] buildWorkbookBytes(String sheetName,
                                                 List<T> data,
                                                 List<ExcelColumn<T>> columns) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet(trimSheetName(sheetName));
            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle bodyStyle = buildBodyStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < columns.size(); index++) {
                Cell cell = headerRow.createCell(index);
                cell.setCellValue(columns.get(index).getHeader());
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(index, 20 * 256);
            }

            List<T> safeData = data == null ? java.util.Collections.emptyList() : data;
            for (int rowIndex = 0; rowIndex < safeData.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                T item = safeData.get(rowIndex);
                for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                    Cell cell = row.createCell(columnIndex);
                    Object value = columns.get(columnIndex).getGetter().apply(item);
                    cell.setCellValue(formatCellValue(value));
                    cell.setCellStyle(bodyStyle);
                }
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("导出 Excel 失败", ex);
        }
    }

    private static CellStyle buildHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static CellStyle buildBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private static String trimSheetName(String sheetName) {
        String safeName = Objects.toString(sheetName, "数据导出");
        if (safeName.length() <= 31) {
            return safeName;
        }
        return safeName.substring(0, 31);
    }

    private static String formatCellValue(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof LocalDateTime) {
            return DATE_TIME_FORMATTER.format((LocalDateTime) value);
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).stripTrailingZeros().toPlainString();
        }
        if (value instanceof List) {
            List<?> values = (List<?>) value;
            if (values.isEmpty()) {
                return "-";
            }
            return values.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(LabWorkflowConstants::translateWorkflowText)
                    .reduce((left, right) -> left + "，" + right)
                    .orElse("-");
        }
        String text = LabWorkflowConstants.translateWorkflowText(String.valueOf(value));
        return text.trim().isEmpty() ? "-" : text;
    }

    /**
     * Excel 导出列定义。
     *
     * @param <T> 数据类型
     */
    public static final class ExcelColumn<T> {

        private final String header;
        private final Function<T, Object> getter;

        private ExcelColumn(String header, Function<T, Object> getter) {
            this.header = header;
            this.getter = getter;
        }

        public String getHeader() {
            return header;
        }

        public Function<T, Object> getGetter() {
            return getter;
        }
    }
}
