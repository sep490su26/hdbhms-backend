package com.sep490.hdbhms.billingandpayment.infrastructure.adapter;


import com.sep490.hdbhms.billingandpayment.application.port.out.ExternalPaymentPort;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.config.PayOSProperties;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.PaymentRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.PaymentIntent;
import com.sep490.hdbhms.shared.id.SnowflakeIdGenerator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "payos")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PayOSAdapter implements ExternalPaymentPort {
    PayOSProperties payOSProperties;
    SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    public PaymentIntent createCheckoutRequest(PaymentRequest request) {
        validatePayOSConfig();
        Long orderCode = snowflakeIdGenerator.next();
        CreatePaymentLinkRequest createPaymentLinkRequest = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(request.amount())
                .description(request.description())
                .returnUrl(payOSProperties.getReturnUrl())
                .cancelUrl(payOSProperties.getCancelUrl())
                .expiredAt(toEpochSeconds(request.expiresAt()))
                .build();
        try {
            CreatePaymentLinkResponse createPaymentLinkResponse = payOSProperties.payOS()
                    .paymentRequests()
                    .create(createPaymentLinkRequest);
            return new PaymentIntent(
                    request.paymentId(),
                    createPaymentLinkResponse.getCheckoutUrl(),
                    PaymentIntentProvider.PAYOS,
                    PaymentStatus.PENDING,
                    createPaymentLinkResponse.getAmount(),
                    String.valueOf(createPaymentLinkResponse.getOrderCode()),
                    createPaymentLinkResponse.getDescription(),
                    createPaymentLinkResponse.getQrCode(),
                    createPaymentLinkResponse.getQrCode(),
                    fromEpochSeconds(createPaymentLinkResponse.getExpiredAt(), request.expiresAt()),
                    createPaymentLinkResponse.getOrderCode(),
                    createPaymentLinkResponse.getPaymentLinkId()
            );
        } catch (PayOSException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public PaymentStatus checkPaymentStatus(String paymentId) {
        return null;
    }

    private void validatePayOSConfig() {
        if (!StringUtils.hasText(payOSProperties.getClientId())
                || !StringUtils.hasText(payOSProperties.getApiKey())
                || !StringUtils.hasText(payOSProperties.getChecksumKey())
                || !StringUtils.hasText(payOSProperties.getReturnUrl())
                || !StringUtils.hasText(payOSProperties.getCancelUrl())) {
            throw new IllegalStateException(
                    "Missing PayOS configuration. Required env: PAYOS_CLIENT_ID, PAYOS_API_KEY, PAYOS_CHECKSUM_KEY, PAYOS_RETURN_URL, PAYOS_CANCEL_URL."
            );
        }
    }

    private Long toEpochSeconds(LocalDateTime expiresAt) {
        return expiresAt == null
                ? null
                : expiresAt.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private LocalDateTime fromEpochSeconds(Long expiredAt, LocalDateTime fallback) {
        if (expiredAt == null) {
            return fallback;
        }
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(expiredAt),
                ZoneId.systemDefault()
        );
    }
}
