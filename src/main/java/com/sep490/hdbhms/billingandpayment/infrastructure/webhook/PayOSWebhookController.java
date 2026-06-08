package com.sep490.hdbhms.billingandpayment.infrastructure.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.billingandpayment.application.port.in.command.ReconcilePaymentCommand;
import com.sep490.hdbhms.billingandpayment.application.port.in.query.GetPaymentIntentQuery;
import com.sep490.hdbhms.billingandpayment.application.port.in.usecase.GetPaymentIntentUseCase;
import com.sep490.hdbhms.billingandpayment.application.port.in.usecase.ReconcilePaymentUseCase;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.TransactionProvider;
import com.sep490.hdbhms.billingandpayment.infrastructure.config.PayOSProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhook/payos")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PayOSWebhookController {
    PayOSProperties properties;
    ObjectMapper objectMapper;
    GetPaymentIntentUseCase getPaymentIntentUseCase;
    ReconcilePaymentUseCase reconcilePaymentUseCase;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody Webhook payload
    ) {
        WebhookData webhookData = properties.payOS().webhooks().verify(payload);
        Long orderCode = webhookData.getOrderCode();

        PaymentIntent paymentIntent;
        try {
            paymentIntent = getPaymentIntentUseCase.execute(new GetPaymentIntentQuery(orderCode));
        } catch (RuntimeException ex) {
            log.warn("PayOS webhook references unknown payment intent. orderCode={}, paymentLinkId={}",
                    orderCode,
                    webhookData.getPaymentLinkId());
            return ResponseEntity.ok().build();
        }

        if (paymentIntent.getStatus() != PaymentIntentStatus.PENDING) {
            return ResponseEntity.ok().build();
        }
        if (StringUtils.hasText(webhookData.getCode()) && !"00".equals(webhookData.getCode())) {
            log.info("Ignore non-success PayOS webhook. orderCode={}, code={}, desc={}",
                    orderCode,
                    webhookData.getCode(),
                    webhookData.getDesc());
            return ResponseEntity.ok().build();
        }

        ReconcilePaymentCommand command = null;
        try {
            command = ReconcilePaymentCommand.builder()
                    .paymentIntentId(paymentIntent.getId())
                    .provider(TransactionProvider.PAYOS)
                    .providerTransactionId(resolveProviderTransactionId(webhookData))
                    .amount(webhookData.getAmount())
                    .content(resolveContent(webhookData))
                    .payerName(webhookData.getCounterAccountName())
                    .payerAccount(webhookData.getCounterAccountNumber())
                    .transactionTime(parseTransactionDateTime(webhookData.getTransactionDateTime()))
                    .rawPayload(objectMapper.writeValueAsString(payload))
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        reconcilePaymentUseCase.execute(command);
        return ResponseEntity.ok().build();
    }

    private String resolveProviderTransactionId(WebhookData webhookData) {
        if (StringUtils.hasText(webhookData.getReference())) {
            return webhookData.getReference();
        }
        if (StringUtils.hasText(webhookData.getPaymentLinkId())) {
            return "PAYOS-" + webhookData.getOrderCode() + "-" + webhookData.getPaymentLinkId();
        }
        return "PAYOS-" + webhookData.getOrderCode();
    }

    private String resolveContent(WebhookData webhookData) {
        if (StringUtils.hasText(webhookData.getDescription())) {
            return webhookData.getDescription();
        }
        if (StringUtils.hasText(webhookData.getDesc())) {
            return webhookData.getDesc();
        }
        return String.valueOf(webhookData.getOrderCode());
    }

    private LocalDateTime parseTransactionDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (RuntimeException ignored) {
                // Try the next known PayOS timestamp format.
            }
        }
        return LocalDateTime.now();
    }
}

