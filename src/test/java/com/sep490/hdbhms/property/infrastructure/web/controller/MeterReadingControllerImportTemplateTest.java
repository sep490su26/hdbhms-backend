package com.sep490.hdbhms.property.infrastructure.web.controller;

import com.sep490.hdbhms.property.application.service.GetBatchMeterReadingsService;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.BatchMeterReadingStatusResponse;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MeterReadingControllerImportTemplateTest {

    @Test
    void downloadImportTemplateUsesVietnameseHeadersPeriodSheetAndAllRooms() throws Exception {
        GetBatchMeterReadingsService batchService = mock(GetBatchMeterReadingsService.class);
        when(batchService.getBatchStatus(eq("08-2026"), eq(7L), eq(11L))).thenReturn(
                BatchMeterReadingStatusResponse.builder()
                        .readingPeriod("08-2026")
                        .rooms(List.of(
                                room("P101", "Phòng 101", "pending", 120, null),
                                room("P102", "Phòng 102", "synced", 130, 145),
                                room("P103", "Phòng 103", "warning", 90, 70)
                        ))
                        .build()
        );
        MeterReadingController controller = new MeterReadingController(null, null, batchService, null);

        ResponseEntity<byte[]> response = controller.downloadImportTemplate("08-2026", 7L, 11L);

        assertNotNull(response.getBody());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            assertEquals("Kỳ nhập 08-2026", workbook.getProperties().getCoreProperties().getTitle());
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("Kỳ nhập 08-2026", sheet.getSheetName());

            assertEquals("Mã phòng", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Phòng", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("Chỉ số điện kỳ trước", sheet.getRow(0).getCell(2).getStringCellValue());
            assertEquals("Chỉ số điện mới", sheet.getRow(0).getCell(3).getStringCellValue());
            assertEquals(4, sheet.getRow(0).getLastCellNum());
            assertNull(sheet.getRow(0).getCell(4));

            assertTrue(sheet.isColumnHidden(0));
            assertEquals(16 * 256, sheet.getColumnWidth(0));
            assertEquals(28 * 256, sheet.getColumnWidth(1));
            assertEquals(22 * 256, sheet.getColumnWidth(2));
            assertEquals(20 * 256, sheet.getColumnWidth(3));

            assertEquals("P101", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("Phòng 101", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(120D, sheet.getRow(1).getCell(2).getNumericCellValue());
            assertEquals("#,##0", sheet.getRow(1).getCell(2).getCellStyle().getDataFormatString());
            assertEquals("#,##0", sheet.getRow(1).getCell(3).getCellStyle().getDataFormatString());
            assertEquals(CellType.BLANK, sheet.getRow(1).getCell(3).getCellType());

            assertEquals("P102", sheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals(145D, sheet.getRow(2).getCell(3).getNumericCellValue());
            assertEquals("P103", sheet.getRow(3).getCell(0).getStringCellValue());
            assertEquals(70D, sheet.getRow(3).getCell(3).getNumericCellValue());
            assertNull(sheet.getRow(4));
        }
    }

    private static BatchMeterReadingStatusResponse.RoomBatchStatus room(
            String roomCode,
            String roomName,
            String status,
            long previousReading,
            Integer currentReading
    ) {
        return BatchMeterReadingStatusResponse.RoomBatchStatus.builder()
                .roomCode(roomCode)
                .roomName(roomName)
                .status(status)
                .electricityPrevious(BigDecimal.valueOf(previousReading))
                .electricityCurrent(currentReading == null ? null : BigDecimal.valueOf(currentReading))
                .build();
    }
}
