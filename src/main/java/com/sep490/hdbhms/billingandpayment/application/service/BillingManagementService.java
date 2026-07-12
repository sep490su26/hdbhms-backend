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
import com.sep490.hdbhms.occupancy.domain.value_objects.ContractEventType;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BillingManagementService {
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

        return new ManualPaymentResponse(toInvoiceResponse(invoice), toPaymentHistory(allocation));
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

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
