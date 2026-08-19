package com.sep490.hdbhms.property.application.service;

import com.sep490.hdbhms.property.domain.model.MeterReading;
import com.sep490.hdbhms.property.domain.value_objects.MeterReadingReviewStatus;
import com.sep490.hdbhms.property.domain.value_objects.AnomalyType;
import com.sep490.hdbhms.property.domain.value_objects.ReadingSource;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.MeterReadingAnomalyEntity;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmitMeterReadingServiceExcelImportParserTest {

    @Test
    void parseExcelPrefersHiddenRoomCodeColumnFromTemplateHeaders() throws Exception {
        List<?> rows = parseExcel(workbookBytes(0));

        assertEquals(1, rows.size());
        assertEquals("P101", recordValue(rows.get(0), "roomCode"));
        assertEquals(new BigDecimal("123"), recordValue(rows.get(0), "currentValue"));
    }

    @Test
    void parseExcelFindsHeaderBelowTitleRow() throws Exception {
        List<?> rows = parseExcel(workbookBytes(1));

        assertEquals(1, rows.size());
        assertEquals("P101", recordValue(rows.get(0), "roomCode"));
        assertEquals(new BigDecimal("123"), recordValue(rows.get(0), "currentValue"));
    }

    @Test
    void parseExcelTreatsTemplateThousandsSeparatorAsGrouping() throws Exception {
        List<?> rows = parseExcel(workbookBytes(0, "3,094"));

        assertEquals(1, rows.size());
        assertEquals("P101", recordValue(rows.get(0), "roomCode"));
        assertEquals(new BigDecimal("3094"), recordValue(rows.get(0), "currentValue"));
    }

    @Test
    void parseExcelTreatsVietnameseThousandsSeparatorAsGrouping() throws Exception {
        List<?> rows = parseExcel(workbookBytes(0, "3.094"));

        assertEquals(1, rows.size());
        assertEquals("P101", recordValue(rows.get(0), "roomCode"));
        assertEquals(new BigDecimal("3094"), recordValue(rows.get(0), "currentValue"));
    }

    @Test
    void parseExcelReadsOptionalRolloverCount() throws Exception {
        List<?> rows = parseExcel(workbookBytesWithRolloverCount(1));

        assertEquals(1, recordValue(rows.get(0), "rolloverCount"));
    }

    @Test
    void excelImportDoesNotPreserveApprovedReviewForUnchangedValue() throws Exception {
        MeterReading reading = MeterReading.builder()
                .previousValue(new BigDecimal("100"))
                .currentValue(new BigDecimal("100"))
                .reviewStatus(MeterReadingReviewStatus.APPROVED)
                .build();
        Method method = SubmitMeterReadingService.class.getDeclaredMethod(
                "isApprovedWithUnchangedValue",
                MeterReading.class,
                BigDecimal.class,
                ReadingSource.class
        );
        method.setAccessible(true);

        assertFalse((Boolean) method.invoke(serviceWithNullDependencies(), reading, new BigDecimal("100"), ReadingSource.EXCEL_IMPORT));
        assertTrue((Boolean) method.invoke(serviceWithNullDependencies(), reading, new BigDecimal("100"), ReadingSource.MANUAL));

        reading.setCurrentValue(new BigDecimal("0"));
        assertFalse((Boolean) method.invoke(serviceWithNullDependencies(), reading, new BigDecimal("0"), ReadingSource.MANUAL));
    }

    @Test
    void negativeUsageCannotBeMarkedAsReviewed() throws Exception {
        Method method = SubmitMeterReadingService.class.getDeclaredMethod(
                "assertAnomaliesCanBeResolved",
                List.class
        );
        method.setAccessible(true);
        List<MeterReadingAnomalyEntity> anomalies = List.of(
                MeterReadingAnomalyEntity.builder().anomalyType(AnomalyType.NEGATIVE_USAGE).build()
        );

        Exception error = assertThrows(Exception.class,
                () -> method.invoke(serviceWithNullDependencies(), anomalies));
        assertEquals(ApiErrorCode.INVALID_METER_READING_VALUE,
                ((java.lang.reflect.InvocationTargetException) error).getCause() instanceof AppException appException
                        ? appException.getApiErrorCode()
                        : null);
    }

    @SuppressWarnings("unchecked")
    private List<?> parseExcel(byte[] workbookBytes) throws Exception {
        SubmitMeterReadingService service = serviceWithNullDependencies();
        MultipartFile file = new MockMultipartFile(
                "file",
                "template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes
        );
        Method parseExcel = SubmitMeterReadingService.class.getDeclaredMethod("parseExcel", MultipartFile.class);
        parseExcel.setAccessible(true);
        return (List<?>) parseExcel.invoke(service, file);
    }

    private SubmitMeterReadingService serviceWithNullDependencies() {
        return new SubmitMeterReadingService(
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, new MeterUsageCalculator(), null
        );
    }

    private Object recordValue(Object record, String accessorName) throws Exception {
        Method accessor = record.getClass().getDeclaredMethod(accessorName);
        accessor.setAccessible(true);
        return accessor.invoke(record);
    }

    private byte[] workbookBytes(int titleRows) throws Exception {
        return workbookBytes(titleRows, 123);
    }

    private byte[] workbookBytes(int titleRows, Object currentReading) throws Exception {
        return workbookBytes(titleRows, currentReading, null);
    }

    private byte[] workbookBytesWithRolloverCount(int rolloverCount) throws Exception {
        return workbookBytes(0, 3, rolloverCount);
    }

    private byte[] workbookBytes(int titleRows, Object currentReading, Integer rolloverCount) throws Exception {
        try (
                XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.createSheet("Ky nhap");
            if (titleRows > 0) {
                sheet.createRow(0).createCell(0).setCellValue("Ky nhap 08-2026");
            }

            int headerIndex = titleRows;
            Row header = sheet.createRow(headerIndex);
            header.createCell(0).setCellValue("Mã phòng");
            header.createCell(1).setCellValue("Phòng");
            header.createCell(2).setCellValue("Chỉ số điện kỳ trước");
            header.createCell(3).setCellValue("Chỉ số điện mới");

            if (rolloverCount != null) {
                header.createCell(4).setCellValue("rolloverCount");
            }

            Row data = sheet.createRow(headerIndex + 1);
            data.createCell(0).setCellValue("P101");
            data.createCell(1).setCellValue("Phòng 101");
            data.createCell(2).setCellValue(100);
            if (currentReading instanceof Number number) {
                data.createCell(3).setCellValue(number.doubleValue());
            } else {
                data.createCell(3).setCellValue(String.valueOf(currentReading));
            }
            if (rolloverCount != null) {
                data.createCell(4).setCellValue(rolloverCount);
            }
            sheet.setColumnHidden(0, true);

            workbook.write(output);
            return output.toByteArray();
        }
    }
}
