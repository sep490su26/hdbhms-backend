package com.sep490.hdbhms.shared.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExcelUtils {
    public static <T> byte[] generateExcel(
            List<T> exportables,
            final List<String> headers,
            final Function<T, List<Object>> rowMapper,
            boolean highData
    ) throws IOException {
        if (highData) {
            try (
                    final SXSSFWorkbook workbook = new SXSSFWorkbook();
                    final ByteArrayOutputStream output = new ByteArrayOutputStream()
            ) {
                final SXSSFSheet sheet = workbook.createSheet("Data");
                final SXSSFRow header = sheet.createRow(0);
                for (int idx = 0; idx < headers.size(); idx++) {
                    header.createCell(idx).setCellValue(headers.get(idx));
                }
                int rowIndex = 1;
                for (T rowData : exportables) {
                    final SXSSFRow row = sheet.createRow(rowIndex++);
                    final List<Object> values = rowMapper.apply(rowData);
                    for (int idx = 0; idx < values.size(); idx++) {
                        setCellValue(row, idx, values.get(idx));
                    }
                }
                workbook.write(output);
                return output.toByteArray();
            }
        }
        try (
                final XSSFWorkbook workbook = new XSSFWorkbook();
                final ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            final XSSFSheet sheet = workbook.createSheet("Data");
            final XSSFRow header = sheet.createRow(0);
            for (int idx = 0; idx < headers.size(); idx++) {
                header.createCell(idx).setCellValue(headers.get(idx));
            }
            int rowIndex = 1;
            for (T rowData : exportables) {
                final XSSFRow row = sheet.createRow(rowIndex++);
                final List<Object> values = rowMapper.apply(rowData);
                for (int idx = 0; idx < values.size(); idx++) {
                    setCellValue(row, idx, values.get(idx));
                }
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static void setCellValue(XSSFRow row, int col, Object value) {
        final XSSFCell cell = row.createCell(col);
        switch (value) {
            case null -> cell.setCellValue("");
            case Number number -> cell.setCellValue((number).doubleValue());
            case Boolean bool -> cell.setCellValue(bool);
            default -> cell.setCellValue(value.toString());
        }
    }

    private static void setCellValue(SXSSFRow row, int col, Object value) {
        final SXSSFCell cell = row.createCell(col);
        switch (value) {
            case null -> cell.setCellValue("");
            case Number number -> cell.setCellValue((number).doubleValue());
            case Boolean bool -> cell.setCellValue(bool);
            default -> cell.setCellValue(value.toString());
        }
    }

}
