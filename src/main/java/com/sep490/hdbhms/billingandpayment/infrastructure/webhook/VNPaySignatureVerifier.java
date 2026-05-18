package com.sep490.hdbhms.billingandpayment.infrastructure.webhook;

import com.sep490.hdbhms.billingandpayment.infrastructure.config.VNPayProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static com.sep490.hdbhms.shared.utils.HashUtils.hmacSHA512;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VNPaySignatureVerifier {
    VNPayProperties vnPayProperties;

    public boolean isValid(Map<String, String> params) {
        log.info(params.toString());
        String receivedHash = params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        var fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        fieldNames.stream().map(fieldName -> URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                .toList().forEach(log::info);
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String value = params.get(fieldName);
            if (value != null && !value.isEmpty()) {
                hashData.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                        .append("=")
                        .append(URLEncoder.encode(value, StandardCharsets.US_ASCII))
                        .append("&");
            }
        }
        if (!hashData.isEmpty()) {
            hashData.deleteCharAt(hashData.length() - 1);
        }

        log.info(hashData.toString());
        var calculatedHash = hmacSHA512(vnPayProperties.getHashSecret(), hashData.toString());
        log.info(calculatedHash);
        log.info(receivedHash);
        return calculatedHash.equals(receivedHash);
    }
}
