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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.time.LocalDateTime;

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
        Long paymentIntentId = webhookData.getOrderCode();
        PaymentIntent paymentIntent = getPaymentIntentUseCase.execute(new GetPaymentIntentQuery(paymentIntentId));
        if (paymentIntent.getStatus() == PaymentIntentStatus.SUCCEEDED) {
            return ResponseEntity.ok().build();
        }

        ReconcilePaymentCommand command = null;
        try {
            command = ReconcilePaymentCommand.builder()
                    .provider(TransactionProvider.BANK)
                    .providerTransactionId(webhookData.getReference())
                    .amount(webhookData.getAmount())
                    .content(webhookData.getDesc())
                    .payerName(webhookData.getCounterAccountName())
                    .payerAccount(webhookData.getCounterAccountNumber())
                    .transactionTime(LocalDateTime.parse(webhookData.getTransactionDateTime()))
                    .rawPayload(objectMapper.writeValueAsString(payload))
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        reconcilePaymentUseCase.execute(command);
        return ResponseEntity.ok().build();
    }
}

