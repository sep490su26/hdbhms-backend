package com.sep490.hdbhms.billingandpayment.application.service;

import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.TransactionHistoryResponse;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class TransactionHistoryExcelTemplateTest {

    @Test
    void loadsInvoiceListTemplate() throws Exception {
        ClassPathResource template = new ClassPathResource("templates/Template danh sách hóa đơn.xlsx");

        try (InputStream inputStream = template.getInputStream(); XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            assertEquals(1, workbook.getNumberOfSheets());
            assertEquals("Danh sách hóa đơn", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
        }
    }

    @Test
    void formatsExcelRowsWithVietnameseLabelsAndDateTimes() throws Exception {
        TransactionHistoryService service = new TransactionHistoryService(mock(JdbcTemplate.class));
        Method generateExcel = TransactionHistoryService.class.getDeclaredMethod("generateExcelFromTemplate", List.class);
        generateExcel.setAccessible(true);
        LocalDateTime dateTime = LocalDateTime.of(2026, 7, 3, 16, 0);
        TransactionHistoryResponse transaction = new TransactionHistoryResponse(
                1L, 2L, "TXN-1", dateTime, 3L, "P101", "Nhà trọ", "Nguyễn Văn A", 200_000L,
                "UTILITY", "UTILITY", "ALLOCATED", "PAYOS", 4L, "INV-1", "Nguyễn Văn A", "Nội dung",
                "2026-07", dateTime, dateTime
        );

        byte[] bytes = (byte[]) generateExcel.invoke(service, List.of(transaction));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var row = workbook.getSheetAt(0).getRow(2);
            assertEquals("Arial", workbook.getFontAt(row.getCell(0).getCellStyle().getFontIndex()).getFontName());
            assertEquals(11, workbook.getFontAt(row.getCell(0).getCellStyle().getFontIndex()).getFontHeightInPoints());
            assertEquals(CellType.NUMERIC, row.getCell(5).getCellType());
            assertEquals("hh:mm dd/MM/yyyy", row.getCell(5).getCellStyle().getDataFormatString());
            assertEquals("Điện nước", row.getCell(8).getStringCellValue());
        }
    }
}
