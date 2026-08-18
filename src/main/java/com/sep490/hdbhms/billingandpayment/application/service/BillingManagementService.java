package com.sep490.hdbhms.billingandpayment.application.service;

import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.TransactionProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.TransactionStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentAllocationEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.RentOverrideEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentAllocationRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentTransactionRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaRentOverrideRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.ApplyRentOverrideRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.ManualPaymentRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.BillingInvoiceLineResponse;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.BillingInvoiceResponse;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.BillingPaymentHistoryResponse;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.ManualPaymentResponse;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.RentOverrideResponse;
import com.sep490.hdbhms.accounting.application.service.ExpenseRequestService;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.billingandpayment.application.port.out.InvoicePaymentNotificationPort;
import com.sep490.hdbhms.notification.application.service.BusinessNotificationPublisher;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.occupancy.domain.value_objects.ContractEventType;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BillingManagementService {
    static final String INVOICE_OVERDUE_EVENT = "INVOICE_OVERDUE";
    static final String INVOICE_TARGET = "INVOICE";
    static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final String INVOICE_EXCEL_TEMPLATE = "templates/Template Hai Dang 1 payment notice.xlsx";
    static final String INVOICE_EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static final int INVOICE_EXCEL_CONTENT_START_ROW = 7;
    static final Map<String, Integer> INVOICE_EXCEL_MAX_ROWS_BY_FLOOR = Map.of(
            "F1", 6,
            "F2", 8,
            "F3", 8,
            "F4", 8,
            "F5", 7
    );
    static final List<String> INVOICE_EXCEL_FLOOR_CODES = List.of("F1", "F2", "F3", "F4", "F5");
    static final List<InvoiceStatus> OVERDUE_WARNING_STATUSES = List.of(
            InvoiceStatus.ISSUED,
            InvoiceStatus.PARTIALLY_PAID,
            InvoiceStatus.OVERDUE
    );
    static final List<NotificationChannel> OVERDUE_WARNING_CHANNELS = List.of(
            NotificationChannel.WEB,
            NotificationChannel.PUSH
    );
    static final List<LeaseStatus> BILLABLE_CONTRACT_STATUSES = List.of(
            LeaseStatus.ACTIVE,
            LeaseStatus.EXPIRING_SOON,
            LeaseStatus.SIGNED,
            LeaseStatus.CONFIRMED
    );

    JpaInvoiceRepository invoiceRepository;
    JpaInvoiceLineRepository invoiceLineRepository;
    JpaPaymentIntentRepository paymentIntentRepository;
    JpaPaymentTransactionRepository paymentTransactionRepository;
    JpaPaymentAllocationRepository paymentAllocationRepository;
    JpaRentOverrideRepository rentOverrideRepository;
    JpaLeaseContractRepository leaseContractRepository;
    JpaRoomRepository roomRepository;
    JpaUserRepository userRepository;
    InvoicePaymentNotificationPort invoicePaymentNotificationPort;
    BusinessNotificationPublisher notificationPublisher;
    JdbcTemplate jdbcTemplate;
    ExpenseRequestService expenseRequestService;

    @Transactional(readOnly = true)
    public List<BillingInvoiceResponse> listInvoices(
            String billingPeriod,
            String status,
            Long propertyId,
            Long roomId,
            String invoiceType
    ) {
        String normalizedPeriod = normalizeOptionalPeriod(billingPeriod);
        InvoiceStatus parsedStatus = parseInvoiceStatus(status);
        InvoiceType parsedType = parseInvoiceType(invoiceType);
        return invoiceRepository.findManagementInvoices(normalizedPeriod, parsedStatus, propertyId, roomId, parsedType)
                .stream()
                .map(this::toInvoiceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExportedFile exportInvoicesAsExcel(
            String billingPeriod,
            String status,
            Long propertyId,
            Long roomId,
            String invoiceType
    ) {
        String normalizedPeriod = normalizeOptionalPeriod(billingPeriod);
        if (normalizedPeriod == null) {
            throw new AppException(ApiErrorCode.BILLING_EXCEL_PERIOD_REQUIRED);
        }
        InvoiceStatus parsedStatus = parseInvoiceStatus(status);
        InvoiceType parsedType = parseInvoiceType(invoiceType);
        List<InvoiceEntity> invoices = invoiceRepository.findManagementInvoices(
                        normalizedPeriod, parsedStatus, propertyId, roomId, parsedType
                ).stream()
                // Keep the exported workbook tied to the selected invoice period even for CHAR columns with padding.
                .filter(invoice -> normalizedPeriod.equals(
                        invoice.getBillingPeriod() == null ? null : invoice.getBillingPeriod().trim()
                ))
                .toList();
        if (invoices.isEmpty()) {
            throw new AppException(ApiErrorCode.BILLING_INVOICE_EXPORT_DATA_EMPTY);
        }

        List<InvoiceExcelRow> rows = buildInvoiceExcelRows(invoices);
        if (rows.isEmpty()) {
            throw new AppException(ApiErrorCode.BILLING_INVOICE_ROOM_EXPORT_DATA_EMPTY);
        }

        try {
            return new ExportedFile(
                    generateInvoiceExcel(rows, normalizedPeriod),
                    INVOICE_EXCEL_CONTENT_TYPE,
                    invoiceExcelFilename(normalizedPeriod)
            );
        } catch (IOException exception) {
            throw new AppException(ApiErrorCode.FAILED_EXPORT_EXCEL);
        }
    }

    private List<InvoiceExcelRow> buildInvoiceExcelRows(List<InvoiceEntity> invoices) {
        Map<Long, InvoiceExcelRowBuilder> grouped = new LinkedHashMap<>();
        Map<Long, Integer> occupantCounts = new LinkedHashMap<>();
        Map<Long, LeaseContractEntity> exportContracts = new LinkedHashMap<>();

        for (InvoiceEntity invoice : invoices) {
            if (invoice.getRoom() == null) {
                continue;
            }
            Long roomId = invoice.getRoom().getId();
            LeaseContractEntity exportContract = resolveExportContract(invoice, occupantCounts, exportContracts);
            int occupantCount = activeOccupantCount(exportContract, occupantCounts);
            if (occupantCount <= 0) {
                continue;
            }
            InvoiceExcelRowBuilder builder = grouped.computeIfAbsent(
                    roomId,
                    ignored -> new InvoiceExcelRowBuilder(
                            invoice,
                            exportContract,
                            occupantCount
                    )
            );
            builder.merge(invoice, exportContract);
        }

        return grouped.values().stream()
                .map(InvoiceExcelRowBuilder::toRow)
                .sorted(Comparator
                        .comparingInt((InvoiceExcelRow row) -> floorNumber(row.floorCode()))
                        .thenComparingInt(row -> roomNumber(row.roomCode())))
                .toList();
    }

    private LeaseContractEntity resolveExportContract(
            InvoiceEntity invoice,
            Map<Long, Integer> occupantCounts,
            Map<Long, LeaseContractEntity> exportContracts
    ) {
        LeaseContractEntity linkedContract = invoice.getLeastContract();
        if (activeOccupantCount(linkedContract, occupantCounts) > 0) {
            return linkedContract;
        }

        Long roomId = invoice.getRoom().getId();
        LeaseContractEntity cachedContract = exportContracts.get(roomId);
        if (cachedContract != null) {
            return cachedContract;
        }

        LeaseContractEntity currentContract = leaseContractRepository
                .findFirstByRoom_IdAndStatusInAndDeletedAtIsNullOrderByIdDesc(
                        roomId,
                        BILLABLE_CONTRACT_STATUSES
                )
                .orElse(null);
        if (currentContract != null) {
            exportContracts.put(roomId, currentContract);
        }
        return currentContract;
    }

    private int activeOccupantCount(LeaseContractEntity contract, Map<Long, Integer> cache) {
        if (contract == null || contract.getId() == null) {
            return 0;
        }
        return cache.computeIfAbsent(
                contract.getId(),
                id -> jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM contract_occupants WHERE contract_id = ? AND status = 'ACTIVE'",
                        Integer.class,
                        id
                )
        );
    }

    private byte[] generateInvoiceExcel(List<InvoiceExcelRow> rows, String billingPeriod) throws IOException {
        ClassPathResource template = new ClassPathResource(INVOICE_EXCEL_TEMPLATE);
        try (
                InputStream inputStream = template.getInputStream();
                XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.getSheetAt(0);
            if (billingPeriod != null) {
                setText(sheet, 5, 0, invoiceExcelTitle(billingPeriod));
            }
            setText(sheet, 6, 16, "Giảm giá (5)");

            Map<String, List<InvoiceExcelRow>> rowsByFloor = new LinkedHashMap<>();
            for (InvoiceExcelRow item : rows) {
                String floorCode = normalizeFloorCode(item.floorCode());
                if (!INVOICE_EXCEL_MAX_ROWS_BY_FLOOR.containsKey(floorCode)) {
                    throw new IOException("Không có khu vực tầng tương ứng trong mẫu cho phòng " + item.roomCode());
                }
                rowsByFloor.computeIfAbsent(floorCode, ignored -> new ArrayList<>()).add(item);
            }

            InvoiceExcelTemplateStyles styles = captureInvoiceExcelTemplateStyles(sheet);
            removeInvoiceExcelContentRows(sheet);

            int sequence = 1;
            int outputRowIndex = INVOICE_EXCEL_CONTENT_START_ROW;
            for (String floorCode : INVOICE_EXCEL_FLOOR_CODES) {
                List<InvoiceExcelRow> floorRows = rowsByFloor.getOrDefault(floorCode, List.of());
                CellStyle[] dataStyles = styles.dataStyles();
                if (floorRows.size() > INVOICE_EXCEL_MAX_ROWS_BY_FLOOR.get(floorCode)) {
                    throw new IOException("Số phòng của tầng " + floorCode + " vượt quá số dòng trong mẫu");
                }
                if (floorRows.isEmpty()) {
                    continue;
                }

                Row floorHeader = createStyledRow(
                        sheet,
                        outputRowIndex++,
                        styles.floorHeaderStyles(),
                        styles.floorHeaderHeight()
                );
                setText(floorHeader, 0, floorTitle(floorRows.get(0)));
                sheet.addMergedRegion(new CellRangeAddress(
                        floorHeader.getRowNum(), floorHeader.getRowNum(), 0, 19
                ));

                for (InvoiceExcelRow item : floorRows) {
                    Row dataRow = createStyledRow(sheet, outputRowIndex++, dataStyles, styles.dataHeight());
                    writeInvoiceExcelRow(dataRow, item, sequence++);

                }
            }

            Row totalRow = createStyledRow(
                    sheet,
                    outputRowIndex,
                    styles.totalStyles(),
                    styles.totalHeight()
            );
            sheet.addMergedRegion(new CellRangeAddress(
                    totalRow.getRowNum(), totalRow.getRowNum(), 0, 1
            ));
            setText(totalRow, 0, "Tổng");
            int firstContentRow = INVOICE_EXCEL_CONTENT_START_ROW + 1;
            int lastDataRow = totalRow.getRowNum();
            setFormula(totalRow, 2, sumFormula("C", firstContentRow, lastDataRow));
            setFormula(totalRow, 3, sumFormula("D", firstContentRow, lastDataRow));
            setFormula(totalRow, 9, sumFormula("J", firstContentRow, lastDataRow));
            setFormula(totalRow, 10, sumFormula("K", firstContentRow, lastDataRow));
            setFormula(totalRow, 13, sumFormula("N", firstContentRow, lastDataRow));
            setFormula(totalRow, 14, sumFormula("O", firstContentRow, lastDataRow));
            setFormula(totalRow, 15, sumFormula("P", firstContentRow, lastDataRow));
            setFormula(totalRow, 16, sumFormula("Q", firstContentRow, lastDataRow));
            setFormula(totalRow, 17, sumFormula("R", firstContentRow, lastDataRow));

            workbook.setPrintArea(
                    workbook.getSheetIndex(sheet),
                    0,
                    19,
                    0,
                    totalRow.getRowNum()
            );
            // Keep formulas in the workbook while also caching their values for web/mobile viewers.
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            workbook.setForceFormulaRecalculation(true);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private InvoiceExcelTemplateStyles captureInvoiceExcelTemplateStyles(Sheet sheet) {
        return new InvoiceExcelTemplateStyles(
                captureRowStyles(sheet.getRow(7), 20),
                sheet.getRow(7).getHeight(),
                captureRowStyles(sheet.getRow(8), 20),
                sheet.getRow(8).getHeight(),
                captureRowStyles(sheet.getRow(49), 20),
                sheet.getRow(49).getHeight()
        );
    }

    private CellStyle[] captureRowStyles(Row source, int columnCount) {
        CellStyle[] styles = new CellStyle[columnCount];
        for (int column = 0; column < columnCount; column++) {
            Cell cell = source.getCell(column);
            if (cell != null) {
                styles[column] = cell.getCellStyle();
            }
        }
        return styles;
    }

    private Row createStyledRow(Sheet sheet, int rowIndex, CellStyle[] styles, short height) {
        Row row = sheet.createRow(rowIndex);
        row.setHeight(height);
        for (int column = 0; column < styles.length; column++) {
            if (styles[column] != null) {
                row.createCell(column).setCellStyle(styles[column]);
            }
        }
        return row;
    }

    private void removeInvoiceExcelContentRows(Sheet sheet) {
        for (int mergeIndex = sheet.getNumMergedRegions() - 1; mergeIndex >= 0; mergeIndex--) {
            if (sheet.getMergedRegion(mergeIndex).getFirstRow() >= INVOICE_EXCEL_CONTENT_START_ROW) {
                sheet.removeMergedRegion(mergeIndex);
            }
        }
        for (int rowIndex = sheet.getLastRowNum(); rowIndex >= INVOICE_EXCEL_CONTENT_START_ROW; rowIndex--) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                sheet.removeRow(row);
            }
        }
    }

    private void writeInvoiceExcelRow(Row row, InvoiceExcelRow item, int sequence) {
        int excelRow = row.getRowNum() + 1;
        setNumber(row, 0, sequence);
        setText(row, 1, item.roomCode());
        setNumber(row, 2, item.occupantCount());
        setNumber(row, 3, item.listedPrice());
        setNumber(row, 4, item.servicePerPerson());
        setNumber(row, 5, item.contractTermMonths());
        setDate(row, 6, item.contractStartDate());
        setDate(row, 7, item.contractEndDate());
        setNumber(row, 8, item.paymentCycleMonths());
        // Use the issued invoice lines so non-cycle months do not show rent/service again.
        setNumber(row, 9, item.rentAmount());
        setNumber(row, 10, item.serviceAmount());
        setNumber(row, 11, item.electricityPrevious());
        setNumber(row, 12, item.electricityCurrent());
        setFormula(row, 13, "M" + excelRow + "-L" + excelRow);
        setFormula(row, 14, "N" + excelRow + "*3500");
        setNumber(row, 15, item.previousDebt());
        setNumber(row, 16, item.discountAmount());
        setFormula(row, 17, "J" + excelRow + "+K" + excelRow + "+O" + excelRow + "+P" + excelRow + "-Q" + excelRow);
        setText(row, 18, item.note());
    }

    private String sumFormula(String column, int firstRow, int lastRow) {
        return "SUM(" + column + firstRow + ":" + column + lastRow + ")";
    }

    private String invoiceExcelTitle(String billingPeriod) {
        YearMonth period = YearMonth.parse(billingPeriod);
        return "Thông báo thu tiền trọ Hải Đăng 1 kỳ "
                + period.format(DateTimeFormatter.ofPattern("MM/yyyy"));
    }

    private String floorTitle(InvoiceExcelRow row) {
        int floor = floorNumber(row.floorCode());
        return floor == Integer.MAX_VALUE ? defaultText(row.floorName(), "TẦNG") : "TẦNG " + floor;
    }

    private String normalizeFloorCode(String floorCode) {
        return floorCode == null ? "" : floorCode.trim().toUpperCase(Locale.ROOT);
    }

    private int floorNumber(String floorCode) {
        String normalized = normalizeFloorCode(floorCode);
        if (normalized.startsWith("F")) {
            try {
                return Integer.parseInt(normalized.substring(1));
            } catch (NumberFormatException ignored) {
                // Fall through to the last sort position for an invalid floor code.
            }
        }
        return Integer.MAX_VALUE;
    }

    private String invoiceExcelFilename(String billingPeriod) {
        return billingPeriod == null
                ? "Thông báo đóng tiền trọ Hải Đăng 1.xlsx"
                : "Thông báo đóng tiền trọ Hải Đăng 1 " + billingPeriod + ".xlsx";
    }

    private void setText(Sheet sheet, int rowIndex, int column, String value) {
        setText(sheet.getRow(rowIndex), column, value);
    }

    private void setText(Row row, int column, String value) {
        if (row != null) {
            row.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(value == null ? "" : value);
        }
    }

    private void setNumber(Row row, int column, Number value) {
        if (row != null) {
            row.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                    .setCellValue(value == null ? 0D : value.doubleValue());
        }
    }

    private void setFormula(Row row, int column, String formula) {
        if (row != null) {
            row.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                    .setCellFormula(formula);
        }
    }

    private void setDate(Row row, int column, LocalDate value) {
        if (row != null) {
            Cell cell = row.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            if (value == null) {
                cell.setBlank();
            } else {
                cell.setCellValue(java.sql.Date.valueOf(value));
            }
        }
    }

    private int roomNumber(String roomCode) {
        try {
            return Integer.parseInt(roomCode == null ? "99999" : roomCode);
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }

    @Transactional
    public void sendAutomaticOverdueWarnings() {
        Map<String, Object> result = processOverdueWarnings(null);
        if (((Number) result.get("outboxCount")).intValue() > 0) {
            // ponytail: one daily overdue reminder; later move cadence to property billing settings.
            log.info("Queued overdue invoice notifications: {}", result);
        }
    }

    @Transactional
    public Map<String, Object> processOverdueWarnings(Long currentUserId) {
        List<InvoiceEntity> invoices = invoiceRepository.findOverdueWarningCandidates(
                LocalDateTime.now(VIETNAM_ZONE),
                OVERDUE_WARNING_STATUSES
        );

        int markedOverdueCount = 0;
        int notifiedInvoiceCount = 0;
        int recipientCount = 0;
        int outboxCount = 0;
        int duplicateCount = 0;

        for (InvoiceEntity invoice : invoices) {
            if (invoice.getStatus() != InvoiceStatus.OVERDUE) {
                invoice.setStatus(InvoiceStatus.OVERDUE);
                invoiceRepository.save(invoice);
                markedOverdueCount++;
            }

            WarningResult warningResult = queueOverdueWarning(invoice, currentUserId, false);
            if (warningResult.outboxCount() > 0) {
                notifiedInvoiceCount++;
            }
            recipientCount += warningResult.recipientCount();
            outboxCount += warningResult.outboxCount();
            duplicateCount += warningResult.duplicateCount();
        }

        return Map.of(
                "scannedInvoiceCount", invoices.size(),
                "markedOverdueCount", markedOverdueCount,
                "notifiedInvoiceCount", notifiedInvoiceCount,
                "recipientCount", recipientCount,
                "outboxCount", outboxCount,
                "duplicateSkippedCount", duplicateCount
        );
    }

    @Transactional
    public BillingInvoiceResponse mockMakeInvoiceOverdue(Long invoiceId, Integer daysPastDue) {
        if (invoiceId == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        int days = daysPastDue == null || daysPastDue < 1 ? 1 : daysPastDue;
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.VOIDED) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (safe(invoice.getRemainingAmount()) <= 0) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        invoice.setDueDate(now.minusDays(days));
        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            invoice.setStatus(InvoiceStatus.ISSUED);
            invoice.setIssuedAt(now);
        }
        return toInvoiceResponse(invoiceRepository.saveAndFlush(invoice));
    }

    @Transactional
    public RentOverrideResponse applyRentOverride(ApplyRentOverrideRequest request, Long currentUserId) {
        if (request == null || request.roomId() == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        YearMonth period = requirePeriod(request.billingPeriod());
        var room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));

        InvoiceEntity invoice = invoiceRepository
                .findFirstByRoom_IdAndBillingPeriodAndInvoiceTypeAndStatusNotOrderByIdDesc(
                        request.roomId(),
                        period.toString(),
                        InvoiceType.RENT,
                        InvoiceStatus.VOIDED
                )
                .orElse(null);

        if (invoice != null && invoice.getStatus() == InvoiceStatus.PAID) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        LeaseContractEntity contract = invoice != null && invoice.getLeastContract() != null
                ? invoice.getLeastContract()
                : leaseContractRepository
                .findFirstByRoom_IdAndStatusInAndDeletedAtIsNullOrderByIdDesc(request.roomId(), BILLABLE_CONTRACT_STATUSES)
                .orElseThrow(() -> new AppException(ApiErrorCode.INVALID_REQUEST));

        long listedRent = safe(contract.getMonthlyRent());
        if (listedRent <= 0) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        long invoiceRentSubtotal = invoice == null
                ? listedRent
                : invoiceLineRepository.findByInvoice_IdOrderByIdAsc(invoice.getId()).stream()
                .filter(line -> line.getLineType() == InvoiceLineType.ROOM_RENT)
                .mapToLong(this::lineAmount)
                .sum();
        long discountAmount = resolveDiscountAmount(request, listedRent, Math.max(listedRent, invoiceRentSubtotal));

        RentOverrideEntity rentOverride = rentOverrideRepository
                .findByContract_IdAndBillingPeriod(contract.getId(), period.toString())
                .orElseGet(RentOverrideEntity::new);

        long oldRent = rentOverride.getOverrideMonthlyRent() != null
                ? rentOverride.getOverrideMonthlyRent()
                : listedRent;

        rentOverride.setContract(contract);
        rentOverride.setBillingPeriod(period.toString());
        rentOverride.setOverrideMonthlyRent(Math.max(listedRent - Math.min(discountAmount, listedRent), 0L));
        rentOverride.setDiscountAmount(discountAmount);
        rentOverride.setReason(defaultText(request.reason(), "Điều chỉnh giá tháng " + period));
        rentOverride.setApprovedBy(userRepository.getReferenceById(currentUserId));
        rentOverride = rentOverrideRepository.saveAndFlush(rentOverride);

        boolean invoiceApplied = false;
        if (invoice != null) {
            applyRentDiscountToInvoice(invoice, contract, discountAmount);
            cancelPendingPaymentIntents(invoice);
            invoiceApplied = true;
        }

        return new RentOverrideResponse(
                rentOverride.getId(),
                room.getId(),
                room.getRoomCode(),
                contract.getId(),
                period.toString(),
                oldRent,
                rentOverride.getOverrideMonthlyRent(),
                discountAmount,
                rentOverride.getReason(),
                invoiceApplied,
                invoice == null ? null : invoice.getId(),
                invoice == null || invoice.getStatus() == null ? null : invoice.getStatus().name(),
                rentOverride.getCreatedAt()
        );
    }

    @Transactional
    public ManualPaymentResponse confirmManualPayment(Long invoiceId, ManualPaymentRequest request, Long currentUserId) {
        if (invoiceId == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        long amount = requirePositiveAmount(request == null ? null : request.amount(), "Số tiền không hợp lệ");
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (invoice.getStatus() == InvoiceStatus.DRAFT || invoice.getStatus() == InvoiceStatus.VOIDED) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        long remaining = safe(invoice.getRemainingAmount());
        if (amount > remaining) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();
        PaymentTransactionEntity transaction = paymentTransactionRepository.save(PaymentTransactionEntity.builder()
                .provider(TransactionProvider.CASH)
                .providerTransactionId("MANUAL-" + invoiceId + "-" + System.currentTimeMillis())
                .amount(amount)
                .transactionTime(now.atZone(ZoneId.systemDefault()).toInstant())
                .payerName(defaultText(request == null ? null : request.payerName(), null))
                .content(defaultText(request == null ? null : request.note(), "Xác nhận thanh toán thủ công"))
                .status(TransactionStatus.MATCHED)
                .rawPayload(("manual invoice payment " + invoiceId).getBytes(StandardCharsets.UTF_8))
                .confirmedBy(userRepository.getReferenceById(currentUserId))
                .confirmedAt(now.atZone(ZoneId.systemDefault()).toInstant())
                .build());

        PaymentAllocationEntity allocation = paymentAllocationRepository.saveAndFlush(PaymentAllocationEntity.builder()
                .paymentTransaction(transaction)
                .invoice(invoice)
                .amount(amount)
                .allocatedBy(userRepository.getReferenceById(currentUserId))
                .build());

        long paidAmount = safe(invoice.getPaidAmount()) + amount;
        long nextRemaining = Math.max(safe(invoice.getTotalAmount()) - paidAmount, 0L);
        invoice.setPaidAmount(paidAmount);
        invoice.setRemainingAmount(nextRemaining);
        invoice.setStatus(nextRemaining == 0L ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);
        invoice = invoiceRepository.save(invoice);
        cancelPendingPaymentIntents(invoice);
        notifyInvoicePayment(invoice, amount, currentUserId);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            invoicePaymentNotificationPort.execute(invoice.getId(), amount);
        }
        if (invoice.getStatus() == InvoiceStatus.PAID && invoice.getInvoiceType() == InvoiceType.FINAL_SETTLEMENT) {
            expenseRequestService.syncLiquidationFinalInvoicePaid(
                    invoice.getLeastContract() == null ? null : invoice.getLeastContract().getId()
            );
        }

        return new ManualPaymentResponse(toInvoiceResponse(invoice), toPaymentHistory(allocation));
    }

    @Transactional
    public Map<String, Object> sendOverdueWarning(Long invoiceId, Long currentUserId) {
        if (invoiceId == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        if (invoice.getRoom() == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (!isOverdueOrExpired(invoice)) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        if (invoice.getStatus() != InvoiceStatus.OVERDUE) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
            invoice = invoiceRepository.save(invoice);
        }

        WarningResult result = queueOverdueWarning(invoice, currentUserId, true);

        return Map.of(
                "invoiceId", invoice.getId(),
                "invoiceCode", defaultText(invoice.getInvoiceCode(), "hóa đơn #" + invoice.getId()),
                "recipientCount", result.recipientCount(),
                "outboxCount", result.outboxCount(),
                "duplicateSkippedCount", result.duplicateCount()
        );
    }

    private WarningResult queueOverdueWarning(InvoiceEntity invoice, Long senderUserId, boolean force) {
        List<Long> recipients = findInvoiceTenantRecipientIds(invoice);
        if (recipients.isEmpty()) {
            return new WarningResult(0, 0, 0);
        }

        Map<String, Object> data = invoiceNotificationData(invoice, senderUserId);
        int outboxCount = 0;
        int duplicateCount = 0;

        for (Long recipientId : recipients) {
            if (!force && overdueWarningExistsToday(invoice.getId(), recipientId)) {
                duplicateCount++;
                continue;
            }
            notificationPublisher.publish(INVOICE_OVERDUE_EVENT, recipientId, INVOICE_TARGET, invoice.getId(), data);
            outboxCount += OVERDUE_WARNING_CHANNELS.size();
        }

        return new WarningResult(recipients.size(), outboxCount, duplicateCount);
    }

    private List<Long> findInvoiceTenantRecipientIds(InvoiceEntity invoice) {
        if (invoice == null || invoice.getRoom() == null || invoice.getRoom().getId() == null) {
            return List.of();
        }
        return jdbcTemplate.queryForList("""
                        SELECT DISTINCT u.user_id
                        FROM users u
                        JOIN person_profiles pp
                          ON pp.user_id = u.user_id
                         AND pp.deleted_at IS NULL
                        JOIN (
                            SELECT lc.primary_tenant_profile_id AS tenant_profile_id
                            FROM lease_contracts lc
                            WHERE lc.deleted_at IS NULL
                              AND lc.room_id = ?
                              AND lc.status IN ('SIGNED', 'ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
                            UNION
                            SELECT co.tenant_profile_id AS tenant_profile_id
                            FROM contract_occupants co
                            JOIN lease_contracts lc
                              ON lc.lease_contract_id = co.contract_id
                            WHERE co.status = 'ACTIVE'
                              AND co.tenant_profile_id IS NOT NULL
                              AND lc.deleted_at IS NULL
                              AND lc.room_id = ?
                              AND lc.status IN ('SIGNED', 'ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
                        ) occupied
                          ON occupied.tenant_profile_id = pp.person_profile_id
                        WHERE u.status = 'ACTIVE'
                          AND u.deleted_at IS NULL
                          AND u.role = 'TENANT'
                        ORDER BY u.user_id
                        """,
                Long.class,
                invoice.getRoom().getId(),
                invoice.getRoom().getId()
        );
    }

    private boolean overdueWarningExistsToday(Long invoiceId, Long recipientId) {
        LocalDate today = LocalDate.now(VIETNAM_ZONE);
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1)
                        FROM notification_outbox
                        WHERE event_type = ?
                          AND target_type = ?
                          AND target_id = ?
                          AND recipient_user_id = ?
                          AND created_at >= ?
                          AND created_at < ?
                        """,
                Integer.class,
                INVOICE_OVERDUE_EVENT,
                INVOICE_TARGET,
                invoiceId,
                recipientId,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
        return count != null && count > 0;
    }

    private void notifyInvoicePayment(InvoiceEntity invoice, long paidAmount, Long actorUserId) {
        List<Long> recipients = findInvoiceTenantRecipientIds(invoice);
        if (recipients.isEmpty()) {
            return;
        }
        String eventType = invoice.getStatus() == InvoiceStatus.PAID ? "INVOICE_PAID" : "INVOICE_PARTIALLY_PAID";
        Map<String, Object> data = invoiceNotificationData(invoice, actorUserId);
        data.put("paymentAmount", paidAmount);
        for (Long recipientId : recipients) {
            notificationPublisher.publish(eventType, recipientId, INVOICE_TARGET, invoice.getId(), data);
        }
    }

    private Map<String, Object> invoiceNotificationData(InvoiceEntity invoice, Long actorUserId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("invoiceId", invoice.getId());
        payload.put("invoiceCode", invoice.getInvoiceCode());
        payload.put("invoiceType", invoice.getInvoiceType() == null ? null : invoice.getInvoiceType().name());
        payload.put("billingPeriod", invoice.getBillingPeriod());
        payload.put("period", invoice.getBillingPeriod());
        payload.put("propertyId", invoice.getProperty() == null ? null : invoice.getProperty().getId());
        payload.put("propertyName", invoice.getProperty() == null ? null : invoice.getProperty().getName());
        payload.put("roomId", invoice.getRoom() == null ? null : invoice.getRoom().getId());
        payload.put("roomCode", invoice.getRoom() == null ? null : invoice.getRoom().getRoomCode());
        payload.put("amount", safe(invoice.getTotalAmount()));
        payload.put("paidAmount", safe(invoice.getPaidAmount()));
        payload.put("remainingAmount", safe(invoice.getRemainingAmount()));
        payload.put("dueDate", invoice.getDueDate() == null ? null : invoice.getDueDate().toLocalDate().toString());
        payload.put("status", invoice.getStatus() == null ? null : invoice.getStatus().name());
        payload.put("actorUserId", actorUserId);
        payload.put("targetRoute", "/dashboard/invoices/" + invoice.getId());
        return payload;
    }

    private String overdueTitle(InvoiceEntity invoice) {
        return "Cảnh báo hóa đơn quá hạn " + defaultText(invoice.getInvoiceCode(), "#" + invoice.getId());
    }

    private String overdueBody(InvoiceEntity invoice) {
        String invoiceCode = defaultText(invoice.getInvoiceCode(), "hóa đơn #" + invoice.getId());
        String roomCode = invoice.getRoom() == null ? "" : defaultText(invoice.getRoom().getRoomCode(), invoice.getRoom().getName());
        String propertyName = invoice.getProperty() == null ? "" : defaultText(invoice.getProperty().getName(), "");
        String dueDate = invoice.getDueDate() == null
                ? "đã quá hạn"
                : invoice.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return "Hóa đơn " + invoiceCode
                + (roomCode == null || roomCode.isBlank() ? "" : " của phòng " + roomCode)
                + (propertyName == null || propertyName.isBlank() ? "" : " tại " + propertyName)
                + " đã hết hạn thanh toán từ " + dueDate
                + ". Số tiền còn phải thanh toán: " + formatMoney(safe(invoice.getRemainingAmount()))
                + ". Vui lòng thanh toán sớm hoặc liên hệ quản lý nếu cần hỗ trợ.";
    }

    private void applyRentDiscountToInvoice(
            InvoiceEntity invoice,
            LeaseContractEntity contract,
            long discountAmount
    ) {
        if (invoice.getInvoiceType() != InvoiceType.RENT) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        List<InvoiceLineEntity> lines = invoiceLineRepository.findByInvoice_IdOrderByIdAsc(invoice.getId());
        InvoiceLineEntity rentLine = lines.stream()
                .filter(line -> line.getLineType() == InvoiceLineType.ROOM_RENT)
                .findFirst()
                .orElseThrow(() -> new AppException(ApiErrorCode.INVALID_REQUEST));

        // Keep the contract rent in the line; the reduction belongs to the RENT invoice.
        rentLine.setUnitPrice(safe(contract.getMonthlyRent()));
        invoiceLineRepository.save(rentLine);

        long nextSubtotal = lines.stream().mapToLong(this::lineAmount).sum();
        long rentSubtotal = lines.stream()
                .filter(line -> line.getLineType() == InvoiceLineType.ROOM_RENT)
                .mapToLong(this::lineAmount)
                .sum();
        if (discountAmount < 0 || discountAmount > rentSubtotal) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        long nextTotal = Math.max(nextSubtotal - discountAmount, 0L);
        if (nextTotal < safe(invoice.getPaidAmount())) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        invoice.setSubtotalAmount(nextSubtotal);
        invoice.setDiscountAmount(discountAmount);
        invoice.setTotalAmount(nextTotal);
        invoice.setRemainingAmount(nextTotal - safe(invoice.getPaidAmount()));
        if (invoice.getPaidAmount() != null && invoice.getPaidAmount() > 0) {
            invoice.setStatus(invoice.getRemainingAmount() == 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);
        }
        invoiceRepository.save(invoice);
    }

    private long resolveDiscountAmount(
            ApplyRentOverrideRequest request,
            long listedRent,
            long discountLimit
    ) {
        if (request.discountAmount() != null) {
            long discount = request.discountAmount();
            if (discount < 0 || discount > discountLimit) {
                throw new AppException(ApiErrorCode.INVALID_REQUEST);
            }
            return discount;
        }

        long effectiveRent = requirePositiveAmount(request.overrideMonthlyRent(), "Giá không hợp lệ");
        if (effectiveRent > listedRent) {
            throw new AppException(ApiErrorCode.ROOM_PROMOTIONAL_PRICE_MUST_BE_BELOW_LIST_PRICE);
        }
        return listedRent - effectiveRent;
    }

    private long resolveInvoiceRent(InvoiceEntity invoice, LeaseContractEntity contract) {
        if (invoice != null) {
            return invoiceLineRepository
                    .findFirstByInvoice_IdAndLineTypeOrderByIdAsc(invoice.getId(), InvoiceLineType.ROOM_RENT)
                    .map(this::lineAmount)
                    .orElseGet(() -> safe(contract.getMonthlyRent()));
        }
        return safe(contract.getMonthlyRent());
    }

    private BillingInvoiceResponse toInvoiceResponse(InvoiceEntity invoice) {
        var property = invoice.getProperty();
        var room = invoice.getRoom();
        var contract = invoice.getLeastContract();
        var tenant = contract == null ? null : contract.getPrimaryTenantProfile();
        String discountReason = invoice.getInvoiceType() == InvoiceType.RENT
                && contract != null
                && invoice.getBillingPeriod() != null
                ? rentOverrideRepository.findByContract_IdAndBillingPeriod(
                        contract.getId(), invoice.getBillingPeriod().trim()
                ).map(RentOverrideEntity::getReason).orElse(null)
                : null;
        return new BillingInvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceCode(),
                invoice.getInvoiceType() == null ? null : invoice.getInvoiceType().name(),
                invoice.getInvoiceReason() == null ? null : invoice.getInvoiceReason().name(),
                invoice.getBillingPeriod(),
                invoice.getStatus() == null ? null : invoice.getStatus().name(),
                property == null ? null : property.getId(),
                property == null ? null : property.getName(),
                room == null ? null : room.getId(),
                room == null ? null : room.getRoomCode(),
                contract == null ? null : contract.getId(),
                contract == null ? null : contract.getContractCode(),
                tenant == null ? null : tenant.getFullName(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                invoice.getSubtotalAmount(),
                invoice.getDiscountAmount(),
                discountReason,
                invoice.getTotalAmount(),
                invoice.getPaidAmount(),
                invoice.getRemainingAmount(),
                invoiceLineRepository.findByInvoice_IdOrderByIdAsc(invoice.getId())
                        .stream()
                        .map(this::toLineResponse)
                        .toList(),
                paymentAllocationRepository.findByInvoice_IdOrderByAllocatedAtDesc(invoice.getId())
                        .stream()
                        .map(this::toPaymentHistory)
                        .toList()
        );
    }

    private BillingInvoiceLineResponse toLineResponse(InvoiceLineEntity line) {
        return new BillingInvoiceLineResponse(
                line.getId(),
                line.getLineType() == null ? null : line.getLineType().name(),
                line.getDescription(),
                line.getQuantity(),
                line.getUnitPrice(),
                lineAmount(line),
                line.getMeterReading() == null ? null : line.getMeterReading().getId(),
                line.getMeterReading() == null || line.getMeterReading().getPhotoFile() == null
                        ? null
                        : line.getMeterReading().getPhotoFile().getId(),
                line.getMeterReading() == null || line.getMeterReading().getPreviousValue() == null
                        ? null
                        : line.getMeterReading().getPreviousValue().stripTrailingZeros().toPlainString(),
                line.getMeterReading() == null || line.getMeterReading().getCurrentValue() == null
                        ? null
                        : line.getMeterReading().getCurrentValue().stripTrailingZeros().toPlainString()
        );
    }

    private BillingPaymentHistoryResponse toPaymentHistory(PaymentAllocationEntity allocation) {
        var transaction = allocation.getPaymentTransaction();
        return new BillingPaymentHistoryResponse(
                allocation.getId(),
                transaction == null ? null : transaction.getId(),
                allocation.getAmount(),
                transaction == null || transaction.getProvider() == null ? null : transaction.getProvider().name(),
                transaction == null || transaction.getStatus() == null ? null : transaction.getStatus().name(),
                transaction == null ? null : transaction.getPayerName(),
                transaction == null ? null : transaction.getContent(),
                transaction == null || transaction.getConfirmedBy() == null ? null : transaction.getConfirmedBy().getId(),
                transaction == null ? null : toLocalDateTime(transaction.getConfirmedAt()),
                allocation.getAllocatedBy() == null ? null : allocation.getAllocatedBy().getId(),
                allocation.getAllocatedAt()
        );
    }

    private void cancelPendingPaymentIntents(InvoiceEntity invoice) {
        paymentIntentRepository.findByInvoice_IdAndStatusIn(
                        invoice.getId(),
                        List.of(PaymentIntentStatus.CREATED, PaymentIntentStatus.PENDING)
                )
                .forEach(paymentIntent -> {
                    paymentIntent.setStatus(PaymentIntentStatus.CANCELLED);
                    paymentIntentRepository.save(paymentIntent);
                });
    }

    private void appendContractPriceEvent(
            Long contractId,
            long oldRent,
            long newRent,
            String billingPeriod,
            Long currentUserId
    ) {
        jdbcTemplate.update("""
                        INSERT INTO contract_events (contract_id, event_type, event_data, created_by, created_at)
                        VALUES (?, ?, ?, ?, NOW(6))
                        """,
                contractId,
                ContractEventType.PRICE_CHANGED.name(),
                ("Điều chỉnh tiền thuê tháng " + billingPeriod + ": " + oldRent + " -> " + newRent)
                        .getBytes(StandardCharsets.UTF_8),
                currentUserId
        );
    }

    private YearMonth requirePeriod(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
    }

    private String normalizeOptionalPeriod(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requirePeriod(value).toString();
    }

    private InvoiceStatus parseInvoiceStatus(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return InvoiceStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
    }

    private InvoiceType parseInvoiceType(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return InvoiceType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
    }

    private long requirePositiveAmount(Long amount, String message) {
        if (amount == null || amount <= 0) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        return amount;
    }

    private long lineAmount(InvoiceLineEntity line) {
        if (line == null) {
            return 0L;
        }
        if (line.getAmount() != null) {
            return line.getAmount();
        }
        return safe(line.getUnitPrice()) * (line.getQuantity() == null ? 1L : line.getQuantity());
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean isOverdueOrExpired(InvoiceEntity invoice) {
        if (invoice.getStatus() == InvoiceStatus.OVERDUE) {
            return true;
        }
        return safe(invoice.getRemainingAmount()) > 0
                && invoice.getDueDate() != null
                && invoice.getDueDate().isBefore(LocalDateTime.now());
    }

    private String formatMoney(long value) {
        return java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value) + " VNĐ";
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private final class InvoiceExcelRowBuilder {
        private final Long roomId;
        private final String roomCode;
        private final String floorCode;
        private final String floorName;
        private final int occupantCount;
        private long listedPrice;
        private long rentAmount;
        private long serviceAmount;
        private long electricityAmount;
        private long discountAmount;
        private String discountReason;
        private long totalAmount;
        private BigDecimal electricityPrevious = BigDecimal.ZERO;
        private BigDecimal electricityCurrent = BigDecimal.ZERO;
        private LocalDate contractStartDate;
        private LocalDate contractEndDate;
        private Integer paymentCycleMonths = 1;
        private LocalDateTime latestIssueDate;
        private final List<String> payablePeriods = new ArrayList<>();

        private InvoiceExcelRowBuilder(
                InvoiceEntity invoice,
                LeaseContractEntity exportContract,
                int occupantCount
        ) {
            this.roomId = invoice.getRoom().getId();
            this.roomCode = invoice.getRoom().getRoomCode();
            this.floorCode = invoice.getRoom().getFloor() == null
                    ? null
                    : invoice.getRoom().getFloor().getFloorCode();
            this.floorName = invoice.getRoom().getFloor() == null
                    ? null
                    : invoice.getRoom().getFloor().getName();
            this.occupantCount = occupantCount;
            this.listedPrice = safe(invoice.getRoom().getListedPrice());
            updateContract(exportContract);
        }

        private void merge(InvoiceEntity invoice, LeaseContractEntity exportContract) {
            rentAmount += sumLines(invoice, InvoiceLineType.ROOM_RENT);
            serviceAmount += sumLines(invoice, InvoiceLineType.SERVICE_FEE);
            electricityAmount += sumLines(invoice, InvoiceLineType.ELECTRICITY);
            if (invoice.getInvoiceType() == InvoiceType.RENT) {
                discountAmount += resolveExportDiscount(invoice);
                if (invoice.getLeastContract() != null && invoice.getBillingPeriod() != null) {
                    rentOverrideRepository.findByContract_IdAndBillingPeriod(
                                    invoice.getLeastContract().getId(), invoice.getBillingPeriod().trim()
                            )
                            .map(RentOverrideEntity::getReason)
                            .filter(reason -> reason != null && !reason.isBlank())
                            .ifPresent(reason -> discountReason = reason);
                }
            }
            totalAmount += safe(invoice.getTotalAmount());
            addPayablePeriods(invoice, exportContract);

            if (latestIssueDate == null
                || (invoice.getIssueDate() != null && invoice.getIssueDate().isAfter(latestIssueDate))) {
                latestIssueDate = invoice.getIssueDate();
                updateContract(exportContract);
                InvoiceLineEntity electricityLine = invoiceLineRepository
                        .findByInvoice_IdOrderByIdAsc(invoice.getId())
                        .stream()
                        .filter(line -> line.getLineType() == InvoiceLineType.ELECTRICITY)
                        .findFirst()
                        .orElse(null);
                if (electricityLine != null && electricityLine.getMeterReading() != null) {
                    if (electricityLine.getMeterReading().getPreviousValue() != null) {
                        electricityPrevious = electricityLine.getMeterReading().getPreviousValue();
                    }
                    if (electricityLine.getMeterReading().getCurrentValue() != null) {
                        electricityCurrent = electricityLine.getMeterReading().getCurrentValue();
                    }
                }
            }
        }

        private long resolveExportDiscount(InvoiceEntity invoice) {
            long invoiceDiscount = safe(invoice.getDiscountAmount());
            if (invoiceDiscount > 0L) {
                return invoiceDiscount;
            }
            if (invoice.getLeastContract() == null || invoice.getBillingPeriod() == null) {
                return 0L;
            }
            return rentOverrideRepository.findByContract_IdAndBillingPeriod(
                            invoice.getLeastContract().getId(), invoice.getBillingPeriod().trim()
                    )
                    .map(RentOverrideEntity::getDiscountAmount)
                    .map(BillingManagementService.this::safe)
                    .orElse(0L);
        }

        private long sumLines(InvoiceEntity invoice, InvoiceLineType lineType) {
            return invoiceLineRepository.findByInvoice_IdOrderByIdAsc(invoice.getId()).stream()
                    .filter(line -> line.getLineType() == lineType)
                    .mapToLong(BillingManagementService.this::lineAmount)
                    .sum();
        }

        private void updateContract(LeaseContractEntity contract) {
            if (contract == null) {
                return;
            }
            contractStartDate = contract.getStartDate();
            contractEndDate = contract.getEndDate();
            int configuredCycle = contract.getPaymentCycleMonths() == null ? 1 : contract.getPaymentCycleMonths();
            paymentCycleMonths = occupantCount <= 0 ? 0 : configuredCycle;
        }

        private void addPayablePeriods(InvoiceEntity invoice, LeaseContractEntity contract) {
            if (invoice.getBillingPeriod() == null || invoice.getBillingPeriod().isBlank()) {
                return;
            }
            YearMonth start = YearMonth.parse(invoice.getBillingPeriod().trim());
            boolean cycleCharge = invoiceLineRepository.findByInvoice_IdOrderByIdAsc(invoice.getId()).stream()
                    .anyMatch(line -> line.getLineType() == InvoiceLineType.ROOM_RENT
                            || line.getLineType() == InvoiceLineType.SERVICE_FEE);
            int months = cycleCharge && contract != null && contract.getPaymentCycleMonths() != null
                    ? Math.max(contract.getPaymentCycleMonths(), 1)
                    : 1;
            for (int index = 0; index < months; index++) {
                String period = start.plusMonths(index).toString();
                if (!payablePeriods.contains(period)) {
                    payablePeriods.add(period);
                }
            }
        }

        private InvoiceExcelRow toRow() {
            BigDecimal usage = electricityCurrent.subtract(electricityPrevious);
            long serviceUnits = (long) occupantCount * Math.max(paymentCycleMonths, 1);
            long servicePerPerson = serviceUnits <= 0 ? 0L : serviceAmount / serviceUnits;
            int contractTermMonths = contractStartDate == null || contractEndDate == null
                    ? 0
                    : (int) ChronoUnit.MONTHS.between(
                    contractStartDate.withDayOfMonth(1), contractEndDate.withDayOfMonth(1)
            ) + 1;
            return new InvoiceExcelRow(
                    roomId,
                    roomCode,
                    floorCode,
                    floorName,
                    occupantCount,
                    listedPrice,
                    servicePerPerson,
                    contractTermMonths,
                    contractStartDate,
                    contractEndDate,
                    paymentCycleMonths,
                    rentAmount,
                    serviceAmount,
                    electricityPrevious,
                    electricityCurrent,
                    usage.max(BigDecimal.ZERO),
                    electricityAmount,
                    0L,
                    discountAmount,
                    totalAmount,
                    buildInvoiceNote(payablePeriods, discountReason)
            );
        }

        private String buildInvoiceNote(List<String> periods, String reason) {
            String note = periods.isEmpty() ? "" : "Kỳ tính tiền: " + String.join(", ", periods);
            if (reason == null || reason.isBlank()) {
                return note;
            }
            return note.isBlank() ? "Giảm giá: " + reason : note + " | Giảm giá: " + reason;
        }
    }

    private record InvoiceExcelTemplateStyles(
            CellStyle[] floorHeaderStyles,
            short floorHeaderHeight,
            CellStyle[] dataStyles,
            short dataHeight,
            CellStyle[] totalStyles,
            short totalHeight
    ) {
    }

    private record InvoiceExcelRow(
            Long roomId,
            String roomCode,
            String floorCode,
            String floorName,
            int occupantCount,
            long listedPrice,
            long servicePerPerson,
            int contractTermMonths,
            LocalDate contractStartDate,
            LocalDate contractEndDate,
            int paymentCycleMonths,
            long rentAmount,
            long serviceAmount,
            BigDecimal electricityPrevious,
            BigDecimal electricityCurrent,
            BigDecimal electricityUsage,
            long electricityAmount,
            long previousDebt,
            long discountAmount,
            long totalAmount,
            String note
    ) {
    }

    public record ExportedFile(byte[] bytes, String contentType, String filename) {
    }

    private record WarningResult(int recipientCount, int outboxCount, int duplicateCount) {
    }
}
