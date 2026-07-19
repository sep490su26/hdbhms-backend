package com.sep490.hdbhms.billingandpayment.application.service;

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
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.notification.application.service.BusinessNotificationPublisher;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.occupancy.domain.value_objects.ContractEventType;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    BusinessNotificationPublisher notificationPublisher;
    JdbcTemplate jdbcTemplate;

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

    @Transactional
    public void sendAutomaticOverdueWarnings() {
        Map<String, Object> result = processOverdueWarnings(null);
        if (((Number) result.get("outboxCount")).intValue() > 0) {
            // ponytail: one daily overdue reminder; later move cadence to property billing settings.
            log.info("Queued overdue invoice warnings: {}", result);
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn hóa đơn.");
        }
        int days = daysPastDue == null || daysPastDue < 1 ? 1 : daysPastDue;
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn."));
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.VOIDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể mock quá hạn cho hóa đơn đã thanh toán hoặc đã hủy.");
        }
        if (safe(invoice.getRemainingAmount()) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hóa đơn không còn số tiền phải thu.");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn phòng cần điều chỉnh.");
        }
        YearMonth period = requirePeriod(request.billingPeriod());
        long newRent = requirePositiveAmount(request.overrideMonthlyRent(), "Giá không hợp lệ");
        var room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phòng."));

        InvoiceEntity invoice = invoiceRepository
                .findFirstByRoom_IdAndBillingPeriodAndInvoiceTypeAndStatusNotOrderByIdDesc(
                        request.roomId(),
                        period.toString(),
                        InvoiceType.RENT,
                        InvoiceStatus.VOIDED
                )
                .orElse(null);

        if (invoice != null && invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hóa đơn tháng này đã thanh toán, không thể điều chỉnh");
        }

        LeaseContractEntity contract = invoice != null && invoice.getLeastContract() != null
                ? invoice.getLeastContract()
                : leaseContractRepository
                .findFirstByRoom_IdAndStatusInAndDeletedAtIsNullOrderByIdDesc(request.roomId(), BILLABLE_CONTRACT_STATUSES)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Không có hợp đồng/phòng đang hiệu lực để điều chỉnh giá."
                ));

        RentOverrideEntity rentOverride = rentOverrideRepository
                .findByContract_IdAndBillingPeriod(contract.getId(), period.toString())
                .orElseGet(RentOverrideEntity::new);

        long oldRent = rentOverride.getOverrideMonthlyRent() != null
                ? rentOverride.getOverrideMonthlyRent()
                : resolveInvoiceRent(invoice, contract);

        rentOverride.setContract(contract);
        rentOverride.setBillingPeriod(period.toString());
        rentOverride.setOverrideMonthlyRent(newRent);
        rentOverride.setReason(defaultText(request.reason(), "Điều chỉnh giá tháng " + period));
        rentOverride.setApprovedBy(userRepository.getReferenceById(currentUserId));
        rentOverride = rentOverrideRepository.saveAndFlush(rentOverride);

        boolean invoiceApplied = false;
        if (invoice != null) {
            applyOverrideToInvoice(invoice, newRent, oldRent);
            cancelPendingPaymentIntents(invoice);
            invoiceApplied = true;
        }

        appendContractPriceEvent(contract.getId(), oldRent, newRent, period.toString(), currentUserId);

        return new RentOverrideResponse(
                rentOverride.getId(),
                room.getId(),
                room.getRoomCode(),
                contract.getId(),
                period.toString(),
                oldRent,
                newRent,
                invoiceApplied,
                invoice == null ? null : invoice.getId(),
                invoice == null || invoice.getStatus() == null ? null : invoice.getStatus().name(),
                rentOverride.getCreatedAt()
        );
    }

    @Transactional
    public ManualPaymentResponse confirmManualPayment(Long invoiceId, ManualPaymentRequest request, Long currentUserId) {
        if (invoiceId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn hóa đơn.");
        }
        long amount = requirePositiveAmount(request == null ? null : request.amount(), "Số tiền không hợp lệ");
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn."));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hóa đơn này đã được thanh toán");
        }
        if (invoice.getStatus() == InvoiceStatus.DRAFT || invoice.getStatus() == InvoiceStatus.VOIDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ xác nhận thanh toán cho hóa đơn đã phát hành.");
        }
        long remaining = safe(invoice.getRemainingAmount());
        if (amount > remaining) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền nhận vượt quá số tiền còn lại.");
        }

        LocalDateTime now = LocalDateTime.now();
        PaymentTransactionEntity transaction = paymentTransactionRepository.save(PaymentTransactionEntity.builder()
                .provider(TransactionProvider.CASH)
                .providerTransactionId("MANUAL-" + invoiceId + "-" + System.currentTimeMillis())
                .collectionAccount(invoice.getCollectionAccount())
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

        return new ManualPaymentResponse(toInvoiceResponse(invoice), toPaymentHistory(allocation));
    }

    @Transactional
    public Map<String, Object> sendOverdueWarning(Long invoiceId, Long currentUserId) {
        if (invoiceId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn hóa đơn.");
        }
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn."));
        if (invoice.getRoom() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hóa đơn chưa gắn với phòng.");
        }
        if (!isOverdueOrExpired(invoice)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ gửi cảnh báo cho hóa đơn đã hết hạn.");
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

    private void applyOverrideToInvoice(InvoiceEntity invoice, long newRent, long oldRent) {
        var existingLine = invoiceLineRepository
                .findFirstByInvoice_IdAndLineTypeOrderByIdAsc(invoice.getId(), InvoiceLineType.ROOM_RENT);
        InvoiceLineEntity line = existingLine
                .orElseGet(() -> InvoiceLineEntity.builder()
                        .invoice(invoice)
                        .lineType(InvoiceLineType.ROOM_RENT)
                        .description("Tiền phòng tháng " + invoice.getBillingPeriod())
                        .quantity(1)
                        .unitPrice(oldRent)
                        .collectionAccount(invoice.getCollectionAccount())
                        .build());

        long oldLineAmount = existingLine.map(this::lineAmount).orElse(0L);
        line.setQuantity(1);
        line.setUnitPrice(newRent);
        invoiceLineRepository.save(line);

        long nextSubtotal = Math.max(safe(invoice.getSubtotalAmount()) - oldLineAmount + newRent, 0L);
        long nextTotal = Math.max(nextSubtotal - safe(invoice.getDiscountAmount()), 0L);
        if (nextTotal < safe(invoice.getPaidAmount())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá điều chỉnh nhỏ hơn số tiền đã thanh toán.");
        }
        invoice.setSubtotalAmount(nextSubtotal);
        invoice.setTotalAmount(nextTotal);
        invoice.setRemainingAmount(nextTotal - safe(invoice.getPaidAmount()));
        if (invoice.getPaidAmount() != null && invoice.getPaidAmount() > 0) {
            invoice.setStatus(invoice.getRemainingAmount() == 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);
        }
        invoiceRepository.save(invoice);
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
                lineAmount(line)
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
                ("Monthly rent override " + billingPeriod + ": " + oldRent + " -> " + newRent)
                        .getBytes(StandardCharsets.UTF_8),
                currentUserId
        );
    }

    private YearMonth requirePeriod(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn tháng cần điều chỉnh");
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tháng cần điều chỉnh phải có định dạng yyyy-MM.");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái hóa đơn không hợp lệ.");
        }
    }

    private InvoiceType parseInvoiceType(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return InvoiceType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại hóa đơn không hợp lệ.");
        }
    }

    private long requirePositiveAmount(Long amount, String message) {
        if (amount == null || amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
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

    private record WarningResult(int recipientCount, int outboxCount, int duplicateCount) {
    }
}
