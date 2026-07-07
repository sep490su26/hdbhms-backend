package com.sep490.hdbhms.billingandpayment.infrastructure.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.billingandpayment.application.port.in.command.ReconcilePaymentCommand;
import com.sep490.hdbhms.billingandpayment.application.port.in.usecase.ReconcilePaymentUseCase;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.TransactionProvider;
import com.sep490.hdbhms.billingandpayment.infrastructure.config.PayOSProperties;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentIntentEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentAllocationRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.TenantInvoiceLineResponse;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.TenantInvoiceResponse;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.v2.paymentRequests.Transaction;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/tenant/invoices")
public class TenantInvoiceController {
    JpaInvoiceRepository jpaInvoiceRepository;
    JpaInvoiceLineRepository jpaInvoiceLineRepository;
    JpaPaymentIntentRepository jpaPaymentIntentRepository;
    JpaPaymentAllocationRepository jpaPaymentAllocationRepository;
    ObjectMapper objectMapper;
    PayOSProperties payOSProperties;
    ReconcilePaymentUseCase reconcilePaymentUseCase;

    @GetMapping
    public ApiResponse<List<TenantInvoiceResponse>> getMyInvoices() {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        EnumSet<InvoiceStatus> visibleStatuses = EnumSet.of(
                InvoiceStatus.ISSUED,
                InvoiceStatus.PARTIALLY_PAID,
                InvoiceStatus.PAID,
                InvoiceStatus.OVERDUE
        );
        List<InvoiceEntity> invoices = jpaInvoiceRepository.findTenantVisibleInvoices(userId, visibleStatuses);
        if (syncPayOSPayments(invoices)) {
            invoices = jpaInvoiceRepository.findTenantVisibleInvoices(userId, visibleStatuses);
        }
        return ApiResponse.<List<TenantInvoiceResponse>>builder()
                .data(invoices.stream().map(this::toResponse).toList())
                .build();
    }

    private boolean syncPayOSPayments(List<InvoiceEntity> invoices) {
        boolean synced = false;
        for (InvoiceEntity invoice : invoices) {
            if (invoice.getStatus() == InvoiceStatus.PAID) {
                continue;
            }
            PaymentIntentEntity paymentIntent = jpaPaymentIntentRepository
                    .findFirstByInvoice_IdAndStatusOrderByIdDesc(invoice.getId(), PaymentIntentStatus.PENDING)
                    .orElse(null);
            if (paymentIntent != null && syncPayOSPayment(paymentIntent)) {
                synced = true;
            }
        }
        return synced;
    }

    private boolean syncPayOSPayment(PaymentIntentEntity paymentIntent) {
        if (paymentIntent.getProvider() != PaymentIntentProvider.PAYOS
                || !StringUtils.hasText(paymentIntent.getProviderOrderCode())) {
            return false;
        }
        try {
            PaymentLink paymentLink = payOSProperties.payOS().paymentRequests().get(paymentIntent.getProviderOrderCode());
            if (paymentLink == null || paymentLink.getStatus() != PaymentLinkStatus.PAID) {
                return false;
            }
            Transaction transaction = firstTransaction(paymentLink);
            Long amount = transaction != null && transaction.getAmount() != null
                    ? transaction.getAmount()
                    : paymentLink.getAmountPaid();
            if (amount == null || amount <= 0) {
                return false;
            }
            reconcilePaymentUseCase.execute(ReconcilePaymentCommand.builder()
                    .paymentIntentId(paymentIntent.getId())
                    .provider(TransactionProvider.PAYOS)
                    .providerTransactionId(resolveProviderTransactionId(paymentLink, transaction))
                    .amount(amount)
                    .content(transaction == null ? paymentIntent.getPaymentContent() : transaction.getDescription())
                    .payerName(transaction == null ? null : transaction.getCounterAccountName())
                    .payerAccount(transaction == null ? null : transaction.getCounterAccountNumber())
                    .transactionTime(parseTransactionDateTime(transaction == null ? null : transaction.getTransactionDateTime()))
                    .rawPayload(objectMapper.writeValueAsString(paymentLink))
                    .build());
            return true;
        } catch (vn.payos.exception.APIException exception) {
            log.debug("PayOS payment link not available for tenant invoice sync. paymentIntentId={}, providerOrderCode={}, message={}",
                    paymentIntent.getId(), paymentIntent.getProviderOrderCode(), exception.getMessage());
            return false;
        } catch (Exception exception) {
            log.warn("Could not sync tenant invoice PayOS payment. paymentIntentId={}, providerOrderCode={}, message={}",
                    paymentIntent.getId(), paymentIntent.getProviderOrderCode(), exception.getMessage(), exception);
            return false;
        }
    }

    private Transaction firstTransaction(PaymentLink paymentLink) {
        if (paymentLink.getTransactions() == null || paymentLink.getTransactions().isEmpty()) {
            return null;
        }
        return paymentLink.getTransactions().getFirst();
    }

    private String resolveProviderTransactionId(PaymentLink paymentLink, Transaction transaction) {
        if (transaction != null && StringUtils.hasText(transaction.getReference())) {
            return transaction.getReference();
        }
        if (StringUtils.hasText(paymentLink.getId())) {
            return "PAYOS-INVOICE-POLL-" + paymentLink.getOrderCode() + "-" + paymentLink.getId();
        }
        return "PAYOS-INVOICE-POLL-" + paymentLink.getOrderCode();
    }

    private LocalDateTime parseTransactionDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
        )) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (RuntimeException ignored) {
            }
        }
        return LocalDateTime.now();
    }

    private TenantInvoiceResponse toResponse(InvoiceEntity invoice) {
        RoomEntity room = invoice.getRoom();
        PropertyEntity property = room == null ? invoice.getProperty() : room.getProperty();
        LeaseContractEntity contract = invoice.getLeastContract();
        List<TenantInvoiceLineResponse> lines = jpaInvoiceLineRepository
                .findByInvoice_IdOrderByIdAsc(invoice.getId())
                .stream()
                .map(this::toLineResponse)
                .toList();
        PaymentInfo paymentInfo = paymentInfo(invoice);

        return TenantInvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceCode(invoice.getInvoiceCode())
                .invoiceType(invoice.getInvoiceType() == null ? null : invoice.getInvoiceType().name())
                .billingPeriod(invoice.getBillingPeriod())
                .status(invoice.getStatus() == null ? null : invoice.getStatus().name())
                .propertyId(property == null ? null : property.getId())
                .propertyName(property == null ? null : property.getName())
                .roomId(room == null ? null : room.getId())
                .roomCode(room == null ? null : room.getRoomCode())
                .contractId(contract == null ? null : contract.getId())
                .contractCode(contract == null ? null : contract.getContractCode())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .issuedAt(invoice.getIssuedAt())
                .paidAt(resolvePaidAt(invoice))
                .subtotalAmount(invoice.getSubtotalAmount())
                .discountAmount(invoice.getDiscountAmount())
                .totalAmount(invoice.getTotalAmount())
                .paidAmount(invoice.getPaidAmount())
                .remainingAmount(invoice.getRemainingAmount())
                .paymentIntentId(paymentInfo.paymentIntentId())
                .checkoutUrl(paymentInfo.checkoutUrl())
                .qrCode(paymentInfo.qrCode())
                .providerOrderCode(paymentInfo.providerOrderCode())
                .paymentLinkId(paymentInfo.paymentLinkId())
                .bankBin(paymentInfo.bankBin())
                .bankShortName(paymentInfo.bankShortName())
                .accountNumber(paymentInfo.accountNumber())
                .accountName(paymentInfo.accountName())
                .transferDescription(paymentInfo.transferDescription())
                .lines(lines)
                .build();
    }

    private LocalDateTime resolvePaidAt(InvoiceEntity invoice) {
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            return null;
        }
        return jpaPaymentAllocationRepository.findLatestTransactionTimeByInvoiceId(invoice.getId())
                .map(instant -> LocalDateTime.ofInstant(instant, ZoneId.systemDefault()))
                .orElse(invoice.getUpdatedAt());
    }

    private PaymentInfo paymentInfo(InvoiceEntity invoice) {
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return PaymentInfo.empty();
        }
        if (invoice.getStatus() != InvoiceStatus.ISSUED
                && invoice.getStatus() != InvoiceStatus.PARTIALLY_PAID
                && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            return PaymentInfo.empty();
        }
        return jpaPaymentIntentRepository
                .findFirstByInvoice_IdAndStatusOrderByIdDesc(invoice.getId(), PaymentIntentStatus.PENDING)
                .map(this::toPaymentInfo)
                .orElseGet(PaymentInfo::empty);
    }

    private PaymentInfo toPaymentInfo(PaymentIntentEntity paymentIntent) {
        JsonNode payload = parsePayload(paymentIntent.getQrPayload());
        return new PaymentInfo(
                paymentIntent.getId(),
                text(payload, "checkoutUrl"),
                text(payload, "qrCode"),
                firstNonBlank(text(payload, "providerOrderCode"), paymentIntent.getProviderOrderCode()),
                text(payload, "paymentLinkId"),
                text(payload, "bankBin"),
                text(payload, "bankShortName"),
                text(payload, "accountNumber"),
                text(payload, "accountName"),
                text(payload, "transferDescription")
        );
    }

    private JsonNode parsePayload(String value) {
        if (value == null || value.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private TenantInvoiceLineResponse toLineResponse(InvoiceLineEntity line) {
        long amount = line.getAmount() != null
                ? line.getAmount()
                : (long) (line.getQuantity() == null ? 0 : line.getQuantity()) * (line.getUnitPrice() == null ? 0 : line.getUnitPrice());
        return TenantInvoiceLineResponse.builder()
                .id(line.getId())
                .lineType(line.getLineType() == null ? null : line.getLineType().name())
                .description(line.getDescription())
                .quantity(line.getQuantity())
                .unitPrice(line.getUnitPrice())
                .amount(amount)
                .sourceType(line.getSourceType())
                .sourceId(line.getSourceId())
                .build();
    }

    private record PaymentInfo(
            Long paymentIntentId,
            String checkoutUrl,
            String qrCode,
            String providerOrderCode,
            String paymentLinkId,
            String bankBin,
            String bankShortName,
            String accountNumber,
            String accountName,
            String transferDescription
    ) {
        static PaymentInfo empty() {
            return new PaymentInfo(null, null, null, null, null, null, null, null, null, null);
        }
    }
}
