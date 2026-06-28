package com.sep490.hdbhms.shared.infrastructure.sms.esms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.shared.application.port.out.SmsPort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Primary
@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ESmsSmsAdapter implements SmsPort {
    static final String ESMS_URL = "https://rest.esms.vn/MainService.svc/json/SendMultipleMessage_V4_post_json/";
    static final String SUCCESS_CODE = "100";
    static final String SMS_TYPE_CSKH = "2";
    static final String NON_UNICODE = "0";

    ESmsProperties eSmsProperties;
    ObjectMapper objectMapper;

    @Override
    public void send(String phoneNumber, String message) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            log.info(eSmsProperties.getBrandName());
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("ApiKey", eSmsProperties.getApiKey());
            requestBody.put("SecretKey", eSmsProperties.getSecretKey());
            requestBody.put("Phone", normalizeVietnamPhoneNumber(phoneNumber));
            requestBody.put("Content", message);
            requestBody.put("Brandname", eSmsProperties.getBrandName());
            requestBody.put("SmsType", SMS_TYPE_CSKH);
//            requestBody.put("Sandbox", "1");
            requestBody.put("IsUnicode", containsUnicode(message) ? "1" : NON_UNICODE);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            String response = restTemplate.postForObject(ESMS_URL, request, String.class);

            JsonNode responseJson = objectMapper.readTree(response);
            String codeResult = responseJson.path("CodeResult").asText();
            String errorMessage = responseJson.path("ErrorMessage").asText();
            String smsId = responseJson.path("SMSID").asText();

            if (!SUCCESS_CODE.equals(codeResult)) {
                throw new IllegalStateException("eSMS send failed. CodeResult=" + codeResult + ", ErrorMessage=" + errorMessage);
            }

            log.info("eSMS request accepted. phoneNumber={}, smsId={}, codeResult={}",
                    normalizeVietnamPhoneNumber(phoneNumber), smsId, codeResult);
        } catch (Exception ex) {
            log.error("Failed to send SMS via eSMS. phoneNumber={}", phoneNumber, ex);
            throw new IllegalStateException("Failed to send SMS via eSMS", ex);
        }
    }

    private String normalizeVietnamPhoneNumber(String phoneNumber) {
        String normalized = phoneNumber == null ? "" : phoneNumber.trim();
        if (normalized.startsWith("+84")) {
            return "0" + normalized.substring(3);
        }
        if (normalized.startsWith("84")) {
            return "0" + normalized.substring(2);
        }
        return normalized;
    }

    private boolean containsUnicode(String text) {
        return text != null && text.chars().anyMatch(ch -> ch > 127);
    }
}