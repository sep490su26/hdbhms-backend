package com.sep490.hdbhms.billingandpayment.application.service;

import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.TransactionExportRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.TransactionHistoryResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.ExcelUtils;
import com.sep490.hdbhms.shared.utils.PdfUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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
                payment.content
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
                        ExcelUtils.generateExcel(rows, excelHeaders(), this::toExcelRow, false),
                        EXCEL_CONTENT_TYPE,
                        "lich-su-thanh-toan-" + LocalDate.now() + ".xlsx"
                );
                case "pdf" -> new ExportedFile(
                        PdfUtils.generatePdfTable(rows, pdfHeaders(), this::toPdfRow, "Transaction history"),
                        PDF_CONTENT_TYPE,
                        "lich-su-thanh-toan-" + LocalDate.now() + ".pdf"
                );
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Định dạng xuất không hợp lệ");
            };
        } catch (IOException exception) {
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
                rs.getString("content")
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

    private List<String> excelHeaders() {
        return List.of("Mã GD", "Ngày", "Phòng", "Khách thuê", "Số tiền", "Loại", "Trạng thái");
    }

    private List<Object> toExcelRow(TransactionHistoryResponse item) {
        return List.of(
                text(item.transactionCode()),
                formatDateTime(item.transactionTime()),
                text(item.roomCode()),
                text(item.tenantName()),
                item.amount() == null ? 0L : item.amount(),
                text(item.paymentType()),
                text(item.status())
        );
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
}
