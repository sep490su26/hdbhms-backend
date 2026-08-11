package com.sep490.hdbhms.shared.infrastructure.sms.android_sms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.shared.application.port.out.SmsPort;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Primary
@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AndroidSmsGatewaySmsAdapter implements SmsPort {
    AndroidSmsGatewayProperties androidSmsGatewayProperties;
    ObjectMapper objectMapper;

    @Override
    public void send(String phoneNumber, String message) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String auth = androidSmsGatewayProperties.getUsername() + ":" + androidSmsGatewayProperties.getPassword();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);

            Map<String, Object> textMessage = new LinkedHashMap<>();
            textMessage.put("text", message);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("textMessage", textMessage);
            requestBody.put("phoneNumbers", Collections.singletonList(
                    normalizeVietnamPhoneNumber(phoneNumber)
            ));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    androidSmsGatewayProperties.getServerAddress(),
                    request,
                    String.class
            );
            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                throw new AppException(ApiErrorCode.EXTERNAL_SERVICE_ERROR);
            }
            String responseBody = responseEntity.getBody();
            log.info("SMS sent successfully via sms-gate. phoneNumber={}, response={}", phoneNumber, responseBody);

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Failed to send SMS via sms-gate. phoneNumber={}, status={}, body={}",
                    phoneNumber, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new AppException(ApiErrorCode.EXTERNAL_SERVICE_ERROR, ex);
        } catch (Exception ex) {
            log.error("Failed to send SMS via sms-gate. phoneNumber={}", phoneNumber, ex);
            throw new AppException(ApiErrorCode.EXTERNAL_SERVICE_ERROR, ex);
        }
    }

    private String normalizeVietnamPhoneNumber(String phoneNumber) {
        String normalized = phoneNumber == null ? "" : phoneNumber.trim();
        if (normalized.startsWith("0")) {
            return "+84" + normalized.substring(1);
        }
        return normalized;
    }
}
