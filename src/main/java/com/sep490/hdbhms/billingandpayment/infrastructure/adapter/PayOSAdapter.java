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
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "payos")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PayOSAdapter implements ExternalPaymentPort {
    static final Map<String, String> BANK_SHORT_NAMES = Map.ofEntries(
            Map.entry("970418", "BIDV"),
            Map.entry("970422", "MBBank"),
            Map.entry("970436", "Vietcombank"),
            Map.entry("970415", "VietinBank"),
            Map.entry("970405", "Agribank"),
            Map.entry("970407", "Techcombank"),
            Map.entry("970432", "VPBank"),
            Map.entry("970423", "TPBank"),
            Map.entry("970416", "ACB"),
            Map.entry("970403", "Sacombank"),
            Map.entry("970443", "SHB"),
            Map.entry("970441", "VIB"),
            Map.entry("970437", "HDBank"),
            Map.entry("970448", "OCB"),
            Map.entry("970426", "MSB"),
            Map.entry("970449", "LPBank"),
            Map.entry("970440", "SeABank"),
            Map.entry("970425", "ABBank"),
            Map.entry("970419", "NCB"),
            Map.entry("970412", "PVcomBank")
    );

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
                    createPaymentLinkResponse.getPaymentLinkId(),
                    createPaymentLinkResponse.getBin(),
                    BANK_SHORT_NAMES.get(createPaymentLinkResponse.getBin()),
                    createPaymentLinkResponse.getAccountNumber(),
                    createPaymentLinkResponse.getAccountName(),
                    createPaymentLinkResponse.getDescription()
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
