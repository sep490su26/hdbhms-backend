package com.sep490.hdbhms.billingandpayment.infrastructure.adapter;

import com.sep490.hdbhms.billingandpayment.application.port.out.ExternalPaymentPort;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.config.VNPayProperties;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.PaymentRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.PaymentIntent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.sep490.hdbhms.shared.utils.HashUtils.hmacSHA512;


@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "vnpay")
public class VNPayAdapter implements ExternalPaymentPort {
    VNPayProperties vnPayProperties;

    @Override
    public PaymentIntent createCheckoutRequest(PaymentRequest request) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        params.put("vnp_Amount", String.valueOf(new BigDecimal(request.amount()).multiply(new BigDecimal(100)).longValue()));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_Locale", "en");
        params.put("vnp_TxnRef", request.paymentId());   // Use tracking ID as transaction reference
        params.put("vnp_OrderInfo", "Add balance for user " + request.paymentId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_ReturnUrl", request.returnUrl());
        params.put("vnp_IpAddr", "8.8.8.8"); // Should be actual client IP
        params.put("vnp_CreateDate", new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        params.put("vnp_ExpireDate", new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES))));
//        params.put("vnp_SecureHashType", "HmacSHA512");
        var fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        var hashData = new StringBuilder();
        var query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String value = params.get(fieldName);
            if (value != null && !value.isEmpty()) {
                hashData.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                        .append("=")
                        .append(URLEncoder.encode(value, StandardCharsets.US_ASCII))
                        .append("&");
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                        .append("=")
                        .append(URLEncoder.encode(value, StandardCharsets.US_ASCII))
                        .append("&");
            }
        }
        if (!hashData.isEmpty()) {
            hashData.deleteCharAt(hashData.length() - 1);
        }
        if (!query.isEmpty()) {
            query.deleteCharAt(query.length() - 1);
        }
        String vnpSecureHash = hmacSHA512(vnPayProperties.getHashSecret(), hashData.toString());
        String fullUrl = vnPayProperties.getUrl() + "?" + query + "&vnp_SecureHash=" + vnpSecureHash;


        return new PaymentIntent(
                request.paymentId(),
                fullUrl,
                PaymentIntentProvider.BANK_TRANSFER,
                PaymentStatus.PENDING
        );
    }

    @Override
    public PaymentStatus checkPaymentStatus(String paymentId) {
        return null;
    }
}
