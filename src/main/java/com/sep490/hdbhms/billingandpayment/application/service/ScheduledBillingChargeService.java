package com.sep490.hdbhms.billingandpayment.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PendingBillingChargeStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PendingBillingChargeEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPendingBillingChargeRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScheduledBillingChargeService {
    public static final String SOURCE_PENDING_BILLING_CHARGE = "PENDING_BILLING_CHARGE";
    static final DateTimeFormatter INVOICE_CODE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMddHHmmss");

    JpaPendingBillingChargeRepository pendingBillingChargeRepository;
    JpaInvoiceRepository invoiceRepository;
    JpaInvoiceLineRepository invoiceLineRepository;

    @Transactional
    public PendingBillingChargeEntity scheduleCharge(
            RoomEntity room,
            LeaseContractEntity contract,
            InvoiceLineType lineType,
            String description,
            long amount,
            String sourceType,
            Long sourceId,
            String billingPeriod,
            UserEntity createdBy
    ) {
        if (room == null || contract == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không có hợp đồng/phòng đang hiệu lực để lên lịch thu khách.");
        }
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền phát sinh phải lớn hơn 0.");
        }
        YearMonth period = resolveBillingPeriod(billingPeriod);
        LocalDateTime scheduledIssueAt = period.atDay(1).atTime(8, 0);
        LocalDateTime dueDate = period.atDay(5).atTime(23, 59, 59);
        return pendingBillingChargeRepository.save(PendingBillingChargeEntity.builder()
                .property(room.getProperty())
                .room(room)
                .contract(contract)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .lineType(lineType)
                .description(description)
                .amount(amount)
                .billingPeriod(period.toString())
                .scheduledIssueAt(scheduledIssueAt)
                .dueDate(dueDate)
                .status(PendingBillingChargeStatus.SCHEDULED)
                .createdBy(createdBy)
                .build());
    }

    @Scheduled(cron = "0 0 8 1 * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void createMonthlyDraftInvoices() {
        int created = createDueDraftInvoices(LocalDateTime.now());
        if (created > 0) {
            log.info("Created draft invoices for {} scheduled maintenance/violation charges", created);
        }
    }

    @Transactional
    public int createDueDraftInvoices(LocalDateTime now) {
        List<PendingBillingChargeEntity> dueCharges = pendingBillingChargeRepository
                .findByStatusAndScheduledIssueAtLessThanEqualOrderByContract_IdAscIdAsc(
                        PendingBillingChargeStatus.SCHEDULED,
                        now
                );
        int billed = 0;
        for (PendingBillingChargeEntity charge : dueCharges) {
            try {
                InvoiceEntity invoice = findOrCreateDraftInvoice(charge);
                invoiceLineRepository.save(InvoiceLineEntity.builder()
                        .invoice(invoice)
                        .lineType(charge.getLineType())
                        .description(charge.getDescription())
                        .quantity(1)
                        .unitPrice(charge.getAmount())
                        .sourceType(charge.getSourceType())
                        .sourceId(charge.getSourceId())
                        .build());
                long newSubtotal = safe(invoice.getSubtotalAmount()) + safe(charge.getAmount());
                invoice.setSubtotalAmount(newSubtotal);
                invoice.setTotalAmount(newSubtotal - safe(invoice.getDiscountAmount()));
                invoice.setRemainingAmount(invoice.getTotalAmount() - safe(invoice.getPaidAmount()));
                invoiceRepository.save(invoice);
                charge.setInvoice(invoice);
                charge.setStatus(PendingBillingChargeStatus.BILLED);
                charge.setFailureReason(null);
                pendingBillingChargeRepository.save(charge);
                billed++;
            } catch (RuntimeException exception) {
                charge.setStatus(PendingBillingChargeStatus.FAILED);
                charge.setFailureReason(exception.getMessage());
                pendingBillingChargeRepository.save(charge);
                log.warn("Failed to create draft invoice for pending billing charge {}", charge.getId(), exception);
            }
        }
        return billed;
    }

    public java.util.Optional<PendingBillingChargeEntity> findActiveScheduledCharge(String sourceType, Long sourceId) {
        return pendingBillingChargeRepository.findFirstBySourceTypeAndSourceIdAndStatusInOrderByIdDesc(
                sourceType,
                sourceId,
                List.of(PendingBillingChargeStatus.SCHEDULED, PendingBillingChargeStatus.FAILED)
        );
    }

    private InvoiceEntity findOrCreateDraftInvoice(PendingBillingChargeEntity charge) {
        Long contractId = charge.getContract() == null ? null : charge.getContract().getId();
        if (contractId != null) {
            return invoiceRepository
                    .findFirstByLeastContract_IdAndBillingPeriodAndInvoiceTypeAndStatusOrderByIdDesc(
                            contractId,
                            charge.getBillingPeriod(),
                            InvoiceType.OTHER,
                            InvoiceStatus.DRAFT
                    )
                    .orElseGet(() -> createDraftInvoice(charge));
        }
        return createDraftInvoice(charge);
    }

    private InvoiceEntity createDraftInvoice(PendingBillingChargeEntity charge) {
        return invoiceRepository.save(InvoiceEntity.builder()
                .invoiceCode(buildInvoiceCode(charge))
                .property(charge.getProperty())
                .room(charge.getRoom())
                .leastContract(charge.getContract())
                .invoiceType(InvoiceType.OTHER)
                .billingPeriod(charge.getBillingPeriod())
                .issueDate(charge.getScheduledIssueAt())
                .dueDate(charge.getDueDate())
                .status(InvoiceStatus.DRAFT)
                .subtotalAmount(0L)
                .discountAmount(0L)
                .totalAmount(0L)
                .paidAmount(0L)
                .remainingAmount(0L)
                .createdBy(charge.getCreatedBy())
                .build());
    }

    private String buildInvoiceCode(PendingBillingChargeEntity charge) {
        String period = charge.getBillingPeriod().replace("-", "");
        return "INV-OTHER-" + period + "-" + charge.getSourceId() + "-" + LocalDateTime.now().format(INVOICE_CODE_TIME_FORMAT);
    }

    private YearMonth resolveBillingPeriod(String value) {
        if (value == null || value.isBlank()) {
            return YearMonth.from(LocalDate.now().plusMonths(1));
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kỳ hóa đơn phải có định dạng yyyy-MM.");
        }
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }
}
