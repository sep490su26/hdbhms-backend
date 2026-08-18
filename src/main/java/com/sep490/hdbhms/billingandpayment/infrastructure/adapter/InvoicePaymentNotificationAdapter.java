package com.sep490.hdbhms.billingandpayment.infrastructure.adapter;

import com.sep490.hdbhms.billingandpayment.application.port.out.InvoicePaymentNotificationPort;
import com.sep490.hdbhms.billingandpayment.domain.event.InvoicePaymentNotificationRequestedEvent;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvoicePaymentNotificationAdapter implements InvoicePaymentNotificationPort {
    static final String EVENT_TYPE = "INVOICE_PAYMENT_SUCCESS";
    static final String FINAL_SETTLEMENT_EVENT_TYPE = "FINAL_SETTLEMENT_PAYMENT_SUCCESS";
    static final String TRANSFER_DIFFERENCE_EVENT_TYPE = "TRANSFER_DIFFERENCE_PAYMENT_SUCCESS";
    static final String TARGET_TYPE = "INVOICE";

    JpaInvoiceRepository invoiceRepository;
    JpaUserRepository userRepository;
    JdbcTemplate jdbcTemplate;
    ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void execute(Long invoiceId, Long paymentAmount) {
        if (invoiceId == null) {
            return;
        }

        InvoiceEntity invoice = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoice == null
                || invoice.getStatus() != InvoiceStatus.PAID
                || invoice.getInvoiceType() == InvoiceType.DEPOSIT
                || invoice.getRoom() == null) {
            return;
        }

        String eventType = resolveEventType(invoice);
        Map<Long, UserEntity> recipientsById = userRepository.findAllById(
                        findInvoiceTenantRecipientIds(invoice.getRoom().getId())
                ).stream()
                .collect(java.util.stream.Collectors.toMap(
                        UserEntity::getId,
                        user -> user,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        for (UserEntity recipient : recipientsById.values()) {
            applicationEventPublisher.publishEvent(new InvoicePaymentNotificationRequestedEvent(
                    eventType,
                    invoice.getId(),
                    recipient.getId(),
                    recipient.getEmail(),
                    recipient.getPhone(),
                    fallbackSubject(invoice, eventType),
                    fallbackBody(invoice, paymentAmount, eventType),
                    buildPayload(invoice, paymentAmount)
            ));
        }
    }

    private String resolveEventType(InvoiceEntity invoice) {
        if (invoice.getInvoiceType() == InvoiceType.FINAL_SETTLEMENT) {
            return FINAL_SETTLEMENT_EVENT_TYPE;
        }
        if (invoice.getInvoiceType() == InvoiceType.TRANSFER_DIFFERENCE) {
            return TRANSFER_DIFFERENCE_EVENT_TYPE;
        }
        return EVENT_TYPE;
    }

    private List<Long> findInvoiceTenantRecipientIds(Long roomId) {
        if (roomId == null) {
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
                roomId,
                roomId
        );
    }

    private Map<String, Object> buildPayload(InvoiceEntity invoice, Long paymentAmount) {
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
        payload.put("totalAmount", safe(invoice.getTotalAmount()));
        payload.put("paymentAmount", safe(paymentAmount));
        payload.put("paidAmount", safe(invoice.getPaidAmount()));
        payload.put("remainingAmount", safe(invoice.getRemainingAmount()));
        payload.put("paidAt", LocalDateTime.now());
        payload.put("status", invoice.getStatus().name());
        payload.put("targetRoute", "/dashboard/invoices/" + invoice.getId());
        return payload;
    }

    private String fallbackSubject(InvoiceEntity invoice, String eventType) {
        String prefix = FINAL_SETTLEMENT_EVENT_TYPE.equals(eventType)
                ? "Thanh toán tất toán hợp đồng thành công - "
                : TRANSFER_DIFFERENCE_EVENT_TYPE.equals(eventType)
                ? "Thanh toán chênh lệch chuyển phòng thành công - "
                : "Thanh toán hóa đơn thành công - ";
        return prefix + safeText(invoice.getInvoiceCode(), "#" + invoice.getId());
    }

    private String fallbackBody(InvoiceEntity invoice, Long paymentAmount, String eventType) {
        String body = "Hóa đơn " + safeText(invoice.getInvoiceCode(), "#" + invoice.getId())
                + " của phòng " + safeText(invoice.getRoom().getRoomCode(), "chưa xác định")
                + " đã được thanh toán thành công. Số tiền ghi nhận: " + safe(paymentAmount) + " VNĐ.";
        if (FINAL_SETTLEMENT_EVENT_TYPE.equals(eventType)) {
            return body + " Hệ thống đã ghi nhận để tiếp tục xử lý thanh lý và bàn giao phòng.";
        }
        if (TRANSFER_DIFFERENCE_EVENT_TYPE.equals(eventType)) {
            return body + " Hệ thống đã ghi nhận để tiếp tục xử lý chuyển phòng.";
        }
        return body;
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
