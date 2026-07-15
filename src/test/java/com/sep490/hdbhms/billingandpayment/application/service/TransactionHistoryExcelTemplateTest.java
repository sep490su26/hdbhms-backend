package com.sep490.hdbhms.billingandpayment.application.service;

import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.TransactionExportRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.TransactionHistoryResponse;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void filtersExportsByInvoiceMonthOrYear() throws Exception {
        TransactionHistoryService service = new TransactionHistoryService(mock(JdbcTemplate.class));
        Method buildWhere = TransactionHistoryService.class.getDeclaredMethod(
                "buildWhere",
                TransactionExportRequest.class,
                List.class
        );
        buildWhere.setAccessible(true);

        List<Object> monthParams = new ArrayList<>();
        String monthWhere = (String) buildWhere.invoke(
                service,
                new TransactionExportRequest(
                        null, null, null, null, "MONTH", "2026-07", null, null, null, "excel"
                ),
                monthParams
        );
        assertEquals(" WHERE 1 = 1 AND invoice.billing_period = ?", monthWhere);
        assertEquals(List.of("2026-07"), monthParams);

        List<Object> yearParams = new ArrayList<>();
        String yearWhere = (String) buildWhere.invoke(
                service,
                new TransactionExportRequest(
                        null, null, null, null, "YEAR", null, 2026, null, null, "excel"
                ),
                yearParams
        );
        assertEquals(" WHERE 1 = 1 AND invoice.billing_period LIKE ?", yearWhere);
        assertEquals(List.of("2026-%"), yearParams);

        LocalDate fromDate = LocalDate.of(2026, 7, 1);
        LocalDate toDate = LocalDate.of(2026, 7, 14);
        List<Object> dateRangeParams = new ArrayList<>();
        String dateRangeWhere = (String) buildWhere.invoke(
                service,
                new TransactionExportRequest(
                        null, null, null, null, "DATE_RANGE", null, null, fromDate, toDate, "excel"
                ),
                dateRangeParams
        );
        assertEquals(
                " WHERE 1 = 1 AND invoice.issue_date >= ? AND invoice.issue_date <= ?",
                dateRangeWhere
        );
        assertEquals(List.of(fromDate.atStartOfDay(), toDate.atTime(LocalTime.MAX)), dateRangeParams);
    }

    @Test
    void namesExcelExportsBySelectedInvoicePeriod() throws Exception {
        TransactionHistoryService service = new TransactionHistoryService(mock(JdbcTemplate.class));
        Method excelFilename = TransactionHistoryService.class.getDeclaredMethod(
                "excelFilename",
                TransactionExportRequest.class
        );
        excelFilename.setAccessible(true);

        assertEquals(
                "Hóa đơn tháng 07-2026.xlsx",
                excelFilename.invoke(
                        service,
                        new TransactionExportRequest(
                                null, null, null, null, "MONTH", "2026-07", null, null, null, "excel"
                        )
                )
        );
        assertEquals(
                "Hóa đơn năm 2026.xlsx",
                excelFilename.invoke(
                        service,
                        new TransactionExportRequest(
                                null, null, null, null, "YEAR", null, 2026, null, null, "excel"
                        )
                )
        );
        assertEquals(
                "Hóa đơn từ 01-07-2026 đến 14-07-2026.xlsx",
                excelFilename.invoke(
                        service,
                        new TransactionExportRequest(
                                null,
                                null,
                                null,
                                null,
                                "DATE_RANGE",
                                null,
                                null,
                                LocalDate.of(2026, 7, 1),
                                LocalDate.of(2026, 7, 14),
                                "excel"
                        )
                )
        );
        assertEquals(
                "Danh sách tất cả hóa đơn.xlsx",
                excelFilename.invoke(
                        service,
                        new TransactionExportRequest(
                                null, null, null, null, "ALL", null, null, null, null, "excel"
                        )
                )
        );
    }

    @Test
    void exportQueryUsesOneInvoiceAsTheReportRow() {
        assertTrue(TransactionHistoryService.INVOICE_EXPORT_SELECT.contains(
                "invoice.invoice_id AS payment_allocation_id"
        ));
        assertTrue(TransactionHistoryService.INVOICE_EXPORT_SELECT.contains(
                "invoice.total_amount AS amount"
        ));
        assertTrue(TransactionHistoryService.INVOICE_EXPORT_FROM.contains(
                "SELECT latest_allocation.payment_allocation_id"
        ));
    }
}
