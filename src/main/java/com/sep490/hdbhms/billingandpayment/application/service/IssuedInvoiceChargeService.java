package com.sep490.hdbhms.billingandpayment.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.billingandpayment.application.port.out.ExternalPaymentPort;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentIntentEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.PaymentRequest;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IssuedInvoiceChargeService {
    public static final String SOURCE_MAINTENANCE_TICKET = "MAINTENANCE_TICKET";
    static final DateTimeFormatter CODE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMddHHmmss");

    JpaInvoiceRepository invoiceRepository;
    JpaInvoiceLineRepository invoiceLineRepository;
    JpaPaymentIntentRepository paymentIntentRepository;
    ExternalPaymentPort externalPaymentPort;
    Environment environment;
    ObjectMapper objectMapper;

    @Transactional
    public IssuedChargeResult issueMaintenanceCharge(
            RoomEntity room,
            LeaseContractEntity contract,
            InvoiceLineType lineType,
            String description,
            long amount,
            Long ticketId,
            UserEntity createdBy
    ) {
        if (room == null || contract == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không có hợp đồng/phòng đang hiệu lực để xuất hóa đơn cho khách.");
        }
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền phát sinh phải lớn hơn 0.");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(7);
        InvoiceEntity invoice = invoiceRepository.save(InvoiceEntity.builder()
                .invoiceCode(buildInvoiceCode(ticketId, now))
                .property(room.getProperty())
                .room(room)
                .leastContract(contract)
                .invoiceType(InvoiceType.OTHER)
                .billingPeriod(null)
                .issueDate(now)
                .dueDate(expiresAt)
                .status(InvoiceStatus.ISSUED)
                .subtotalAmount(amount)
                .discountAmount(0L)
                .totalAmount(amount)
                .paidAmount(0L)
                .remainingAmount(amount)
                .issuedAt(now)
                .createdBy(createdBy)
                .build());

        InvoiceLineEntity line = invoiceLineRepository.save(InvoiceLineEntity.builder()
                .invoice(invoice)
                .lineType(lineType)
                .description(description)
                .quantity(1)
                .unitPrice(amount)
                .sourceType(SOURCE_MAINTENANCE_TICKET)
                .sourceId(ticketId)
                .build());

        PaymentIntentEntity paymentIntent = paymentIntentRepository.save(PaymentIntentEntity.builder()
                .invoice(invoice)
                .amount(amount)
                .provider(resolveProvider())
                .paymentContent(invoice.getInvoiceCode())
                .status(PaymentIntentStatus.PENDING)
                .expiresAt(expiresAt)
                .build());

        com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.PaymentIntent checkout;
        try {
            checkout = externalPaymentPort.createCheckoutRequest(new PaymentRequest(
                    paymentIntent.getId(),
                    amount,
                    invoice.getInvoiceCode(),
                    expiresAt
            ));
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không thể khởi tạo thanh toán PayOS. Vui lòng thử lại.");
        }
        paymentIntent.setProviderOrderCode(checkout.providerOrderCode());
        paymentIntent.setQrPayload(toCheckoutPayload(checkout, paymentIntent));
        paymentIntent = paymentIntentRepository.save(paymentIntent);

        return new IssuedChargeResult(invoice, line, paymentIntent, checkout);
    }

    @Transactional
    public DraftChargeResult createMaintenanceChargeDraft(
            RoomEntity room,
            LeaseContractEntity contract,
            InvoiceLineType lineType,
            String description,
            long amount,
            Long ticketId,
            UserEntity createdBy
    ) {
        if (room == null || contract == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không có hợp đồng/phòng đang hiệu lực để tạo hóa đơn nháp cho khách.");
        }
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền phát sinh phải lớn hơn 0.");
        }
        LocalDateTime now = LocalDateTime.now();
        InvoiceEntity invoice = invoiceRepository.save(InvoiceEntity.builder()
                .invoiceCode(buildInvoiceCode(ticketId, now))
                .property(room.getProperty())
                .room(room)
                .leastContract(contract)
                .invoiceType(InvoiceType.OTHER)
                .billingPeriod(null)
                .issueDate(now)
                .dueDate(now)
                .status(InvoiceStatus.DRAFT)
                .subtotalAmount(amount)
                .discountAmount(0L)
                .totalAmount(amount)
                .paidAmount(0L)
                .remainingAmount(amount)
                .createdBy(createdBy)
                .build());

        InvoiceLineEntity line = invoiceLineRepository.save(InvoiceLineEntity.builder()
                .invoice(invoice)
                .lineType(lineType)
                .description(description)
                .quantity(1)
                .unitPrice(amount)
                .sourceType(SOURCE_MAINTENANCE_TICKET)
                .sourceId(ticketId)
                .build());

        return new DraftChargeResult(invoice, line);
    }

    @Transactional
    public IssuedChargeResult issueDraftInvoice(Long invoiceId) {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn."));
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể phát hành hóa đơn nháp.");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime paymentExpiresAt = now.plusDays(7);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(now);
        invoice.setIssueDate(now);
        invoice = invoiceRepository.save(invoice);
        InvoiceEntity issuedInvoice = invoice;

        InvoiceLineEntity line = invoiceLineRepository.findByInvoice_IdOrderByIdAsc(issuedInvoice.getId())
                .stream()
                .findFirst()
                .orElse(null);

        PaymentIntentEntity paymentIntent = paymentIntentRepository
                .findFirstByInvoice_IdAndStatusOrderByIdDesc(issuedInvoice.getId(), PaymentIntentStatus.PENDING)
                .orElseGet(() -> paymentIntentRepository.save(PaymentIntentEntity.builder()
                        .invoice(issuedInvoice)
                        .amount(issuedInvoice.getRemainingAmount())
                        .provider(resolveProvider())
                        .paymentContent(issuedInvoice.getInvoiceCode())
                        .status(PaymentIntentStatus.PENDING)
                        .expiresAt(paymentExpiresAt)
                        .build()));

        com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.PaymentIntent checkout;
        try {
            checkout = externalPaymentPort.createCheckoutRequest(new PaymentRequest(
                    paymentIntent.getId(),
                    paymentIntent.getAmount(),
                    issuedInvoice.getInvoiceCode(),
                    paymentExpiresAt
            ));
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không thể khởi tạo thanh toán PayOS. Vui lòng thử lại.");
        }
        paymentIntent.setProviderOrderCode(checkout.providerOrderCode());
        paymentIntent.setQrPayload(toCheckoutPayload(checkout, paymentIntent));
        paymentIntent = paymentIntentRepository.save(paymentIntent);

        return new IssuedChargeResult(issuedInvoice, line, paymentIntent, checkout);
    }

    private PaymentIntentProvider resolveProvider() {
        return "payos".equalsIgnoreCase(environment.getProperty("app.payment.provider", "vnpay"))
                ? PaymentIntentProvider.PAYOS
                : PaymentIntentProvider.BANK_TRANSFER;
    }

    private String buildInvoiceCode(Long ticketId, LocalDateTime now) {
        return "INV-MNT-" + ticketId + "-" + now.format(CODE_TIME_FORMAT);
    }

    private String toCheckoutPayload(
            com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.PaymentIntent checkout,
            PaymentIntentEntity paymentIntent
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("checkoutUrl", checkout.checkOutUrl());
        payload.put("qrCode", checkout.qrCode());
        payload.put("qrPayload", checkout.qrPayload());
        payload.put("providerOrderCode", checkout.providerOrderCode());
        payload.put("paymentLinkId", checkout.paymentLinkId());
        payload.put("bankBin", checkout.bankBin());
        payload.put("bankShortName", checkout.bankShortName());
        payload.put("accountNumber", checkout.accountNumber());
        payload.put("accountName", checkout.accountName());
        payload.put("transferDescription", checkout.transferDescription());
        payload.put("amount", paymentIntent.getAmount());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể lưu dữ liệu checkout PayOS.", exception);
        }
    }

    public record IssuedChargeResult(
            InvoiceEntity invoice,
            InvoiceLineEntity invoiceLine,
            PaymentIntentEntity paymentIntent,
            com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.PaymentIntent checkout
    ) {
    }

    public record DraftChargeResult(
            InvoiceEntity invoice,
            InvoiceLineEntity invoiceLine
    ) {
    }
}
