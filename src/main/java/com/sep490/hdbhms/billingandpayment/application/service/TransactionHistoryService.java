package com.sep490.hdbhms.billingandpayment.application.service;

import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.TransactionExportRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.TransactionHistoryResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.PdfUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionHistoryService {
    static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static final String PDF_CONTENT_TYPE = "application/pdf";
    static final DateTimeFormatter EXPORT_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    static final DateTimeFormatter EXPORT_FILENAME_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    static final String EXCEL_TEMPLATE_PATH = "templates/Template danh sách hóa đơn.xlsx";
    // The supplied template uses row 0 for the title and row 1 for headers.
    static final int TEMPLATE_DATA_START_ROW_INDEX = 2;
    static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    static final Pattern NON_ASCII = Pattern.compile("[^\\x20-\\x7E]");

    static final String BASE_FROM = """
            FROM payment_allocations allocation
            JOIN payment_transactions payment
              ON payment.payment_transaction_id = allocation.payment_transaction_id
            JOIN invoices invoice
              ON invoice.invoice_id = allocation.invoice_id
            LEFT JOIN rooms invoice_room
              ON invoice_room.room_id = invoice.room_id
            LEFT JOIN deposit_agreements deposit
              ON deposit.deposit_agreement_id = invoice.deposit_agreement_id
            LEFT JOIN rooms deposit_room
              ON deposit_room.room_id = deposit.room_id
            LEFT JOIN properties property
              ON property.property_id = invoice.property_id
            LEFT JOIN lease_contracts contract
              ON contract.lease_contract_id = invoice.lease_contract_id
            LEFT JOIN person_profiles tenant_profile
              ON tenant_profile.person_profile_id = contract.primary_tenant_profile_id
            LEFT JOIN person_profiles depositor_profile
              ON depositor_profile.person_profile_id = deposit.depositor_person_profile_id
            """;

    static final String BASE_SELECT = """
            SELECT
                allocation.payment_allocation_id,
                payment.payment_transaction_id,
                COALESCE(NULLIF(payment.provider_transaction_id, ''), CONCAT(payment.provider, '-', payment.payment_transaction_id)) AS transaction_code,
                payment.transaction_time,
                COALESCE(invoice_room.room_id, deposit_room.room_id) AS room_id,
                COALESCE(invoice_room.room_code, deposit_room.room_code) AS room_code,
                property.name AS property_name,
                COALESCE(tenant_profile.full_name, depositor_profile.full_name, payment.payer_name, '') AS tenant_name,
                allocation.amount,
                invoice.invoice_type,
                payment.status,
                payment.provider,
                invoice.invoice_id,
                invoice.invoice_code,
                payment.payer_name,
                payment.content,
                invoice.billing_period,
                invoice.issue_date,
                invoice.due_date
            """ + BASE_FROM;

    JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public PageResponse<TransactionHistoryResponse> getTransactions(
            TransactionExportRequest request,
            Pageable pageable
    ) {
        var params = new ArrayList<Object>();
        String where = buildWhere(request, params);
        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), 100);
        int pageNumber = Math.max(pageable.getPageNumber(), 0);

        var pageParams = new ArrayList<>(params);
        pageParams.add(pageSize);
        pageParams.add((long) pageNumber * pageSize);

        List<TransactionHistoryResponse> rows = jdbcTemplate.query(
                BASE_SELECT + where + " ORDER BY payment.transaction_time DESC, allocation.payment_allocation_id DESC LIMIT ? OFFSET ?",
                this::mapRow,
                pageParams.toArray()
        );
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " + BASE_FROM + where,
                Long.class,
                params.toArray()
        );
        long totalElements = total == null ? 0L : total;

        return PageResponse.<TransactionHistoryResponse>builder()
                .data(rows)
                .pageSize(pageSize)
                .currentPage(pageNumber + 1)
                .totalElements(totalElements)
                .totalPages(totalElements == 0L ? 0 : (int) Math.ceil((double) totalElements / pageSize))
                .build();
    }

    @Transactional(readOnly = true)
    public ExportedFile exportTransactions(TransactionExportRequest request) {
        List<TransactionHistoryResponse> rows = findAll(request);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa có dữ liệu để xuất");
        }

        String format = request == null || request.format() == null ? "excel" : request.format().trim().toLowerCase(Locale.ROOT);
        try {
            return switch (format) {
                case "excel", "xlsx" -> new ExportedFile(
                        generateExcelFromTemplate(rows),
                        EXCEL_CONTENT_TYPE,
                        "Danh sách hóa đơn " + EXPORT_FILENAME_DATE.format(LocalDate.now()) + ".xlsx"
                );
                case "pdf" -> new ExportedFile(
                        PdfUtils.generatePdfTable(rows, pdfHeaders(), this::toPdfRow, "Transaction history"),
                        PDF_CONTENT_TYPE,
                        "lich-su-thanh-toan-" + LocalDate.now() + ".pdf"
                );
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Định dạng xuất không hợp lệ");
            };
        } catch (IOException | POIXMLException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Xuất file thất bại, vui lòng thử lại");
        }
    }

    private List<TransactionHistoryResponse> findAll(TransactionExportRequest request) {
        var params = new ArrayList<Object>();
        String where = buildWhere(request, params);
        return jdbcTemplate.query(
                BASE_SELECT + where + " ORDER BY payment.transaction_time DESC, allocation.payment_allocation_id DESC",
                this::mapRow,
                params.toArray()
        );
    }

    private String buildWhere(TransactionExportRequest request, List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (request == null) {
            return where.toString();
        }
        if (request.roomId() != null) {
            where.append(" AND COALESCE(invoice_room.room_id, deposit_room.room_id) = ?");
            params.add(request.roomId());
        }
        if (request.tenantName() != null && !request.tenantName().isBlank()) {
            where.append("""
                     AND LOWER(COALESCE(tenant_profile.full_name, depositor_profile.full_name, payment.payer_name, '')) LIKE ?
                    """);
            params.add("%" + request.tenantName().trim().toLowerCase(Locale.ROOT) + "%");
        }
        if (request.fromDate() != null) {
            where.append(" AND payment.transaction_time >= ?");
            params.add(request.fromDate().atStartOfDay());
        }
        if (request.toDate() != null) {
            where.append(" AND payment.transaction_time <= ?");
            params.add(request.toDate().atTime(LocalTime.MAX));
        }
        return where.toString();
    }

    private TransactionHistoryResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        String invoiceType = rs.getString("invoice_type");
        return new TransactionHistoryResponse(
                rs.getLong("payment_allocation_id"),
                rs.getLong("payment_transaction_id"),
                rs.getString("transaction_code"),
                toLocalDateTime(rs, "transaction_time"),
                getLongOrNull(rs, "room_id"),
                rs.getString("room_code"),
                rs.getString("property_name"),
                rs.getString("tenant_name"),
                rs.getLong("amount"),
                paymentType(invoiceType),
                invoiceType,
                rs.getString("status"),
                rs.getString("provider"),
                rs.getLong("invoice_id"),
                rs.getString("invoice_code"),
                rs.getString("payer_name"),
                rs.getString("content"),
                rs.getString("billing_period"),
                toLocalDateTime(rs, "issue_date"),
                toLocalDateTime(rs, "due_date")
        );
    }

    private String paymentType(String invoiceType) {
        if ("DEPOSIT".equals(invoiceType)) {
            return "DEPOSIT";
        }
        if ("RENT".equals(invoiceType)) {
            return "RENT";
        }
        return invoiceType == null || invoiceType.isBlank() ? "OTHER" : invoiceType;
    }

    private byte[] generateExcelFromTemplate(List<TransactionHistoryResponse> rows) throws IOException {
        ClassPathResource template = new ClassPathResource(EXCEL_TEMPLATE_PATH);
        try (
                InputStream inputStream = template.getInputStream();
                XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IOException("Excel template does not contain a worksheet");
            }
            Sheet sheet = workbook.getSheetAt(0);
            ExcelDataStyles styles = createExcelDataStyles(workbook);
            for (int index = 0; index < rows.size(); index++) {
                writeExcelRow(getOrCreateTemplateRow(sheet, TEMPLATE_DATA_START_ROW_INDEX + index), rows.get(index), styles);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private Row getOrCreateTemplateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row != null) {
            return row;
        }

        Row templateRow = sheet.getRow(TEMPLATE_DATA_START_ROW_INDEX);
        row = sheet.createRow(rowIndex);
        if (templateRow != null) {
            row.setHeight(templateRow.getHeight());
            for (int column = 0; column < 9; column++) {
                Cell templateCell = templateRow.getCell(column);
                if (templateCell != null) {
                    row.createCell(column).setCellStyle(templateCell.getCellStyle());
                }
            }
        }
        return row;
    }

    private ExcelDataStyles createExcelDataStyles(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 11);

        CellStyle textStyle = createCellStyle(workbook, font, HorizontalAlignment.LEFT);
        CellStyle amountStyle = createCellStyle(workbook, font, HorizontalAlignment.RIGHT);
        amountStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0 \"đ\""));
        CellStyle dateTimeStyle = createCellStyle(workbook, font, HorizontalAlignment.CENTER);
        dateTimeStyle.setDataFormat(workbook.createDataFormat().getFormat("hh:mm dd/MM/yyyy"));
        return new ExcelDataStyles(textStyle, amountStyle, dateTimeStyle);
    }

    private CellStyle createCellStyle(XSSFWorkbook workbook, Font font, HorizontalAlignment alignment) {
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private void writeExcelRow(Row row, TransactionHistoryResponse item, ExcelDataStyles styles) {
        setTextCell(row, 0, item.transactionCode(), styles.text());
        setTextCell(row, 1, item.tenantName() == null ? item.payerName() : item.tenantName(), styles.text());
        setTextCell(row, 2, item.roomCode(), styles.text());
        setTextCell(row, 3, item.billingPeriod(), styles.text());
        setAmountCell(row, 4, item.amount(), styles.amount());
        setDateTimeCell(row, 5, item.invoiceIssueDate(), styles.dateTime());
        setDateTimeCell(row, 6, item.invoiceDueDate(), styles.dateTime());
        setDateTimeCell(row, 7, item.transactionTime(), styles.dateTime());
        setTextCell(row, 8, invoiceTypeLabel(item.invoiceType()), styles.text());
    }

    private void setTextCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            cell = row.createCell(column);
        }
        cell.setCellStyle(style);
        cell.setCellValue(text(value));
    }

    private void setAmountCell(Row row, int column, Long value, CellStyle style) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            cell = row.createCell(column);
        }
        cell.setCellStyle(style);
        cell.setCellValue(value == null ? 0D : value.doubleValue());
    }

    private void setDateTimeCell(Row row, int column, LocalDateTime value, CellStyle style) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            cell = row.createCell(column);
        }
        cell.setCellStyle(style);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value);
        }
    }

    private String invoiceTypeLabel(String invoiceType) {
        return switch (invoiceType == null ? "" : invoiceType) {
            case "DEPOSIT" -> "Cọc";
            case "RENT" -> "Tiền phòng";
            case "UTILITY" -> "Điện nước";
            case "FINAL_SETTLEMENT" -> "Tất toán";
            case "COMPENSATION" -> "Bồi thường";
            case "OPERATING_REIMBURSEMENT" -> "Hoàn chi";
            case "TRANSFER_DIFFERENCE" -> "Chuyển phòng";
            default -> "Khác";
        };
    }

    private List<String> pdfHeaders() {
        return List.of("Txn", "Date", "Room", "Tenant", "Amount", "Type", "Status");
    }

    private List<String> toPdfRow(TransactionHistoryResponse item) {
        return List.of(
                ascii(item.transactionCode()),
                ascii(formatDateTime(item.transactionTime())),
                ascii(item.roomCode()),
                ascii(item.tenantName()),
                String.valueOf(item.amount() == null ? 0L : item.amount()),
                ascii(item.paymentType()),
                ascii(item.status())
        );
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : EXPORT_DATE_TIME.format(value);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String ascii(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return NON_ASCII.matcher(DIACRITICS.matcher(normalized).replaceAll("")).replaceAll("");
    }

    private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    public record ExportedFile(byte[] bytes, String contentType, String filename) {
    }

    private record ExcelDataStyles(CellStyle text, CellStyle amount, CellStyle dateTime) {
    }
}
