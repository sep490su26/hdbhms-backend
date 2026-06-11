package com.sep490.hdbhms.billingandpayment.infrastructure.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentIntentEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/tenant/invoices")
public class TenantInvoiceController {
    JpaInvoiceRepository jpaInvoiceRepository;
    JpaInvoiceLineRepository jpaInvoiceLineRepository;
    JpaPaymentIntentRepository jpaPaymentIntentRepository;
    ObjectMapper objectMapper;

    @GetMapping
    public ApiResponse<List<TenantInvoiceResponse>> getMyInvoices() {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        List<InvoiceEntity> invoices = jpaInvoiceRepository.findTenantVisibleInvoices(
                userId,
                EnumSet.of(
                        InvoiceStatus.ISSUED,
                        InvoiceStatus.PARTIALLY_PAID,
                        InvoiceStatus.PAID,
                        InvoiceStatus.OVERDUE
                )
        );
        return ApiResponse.<List<TenantInvoiceResponse>>builder()
                .data(invoices.stream().map(this::toResponse).toList())
                .build();
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

    private PaymentInfo paymentInfo(InvoiceEntity invoice) {
        if (invoice.getStatus() == InvoiceStatus.PAID) {
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
