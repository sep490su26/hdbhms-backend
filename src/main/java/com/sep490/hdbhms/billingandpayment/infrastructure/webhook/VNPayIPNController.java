package com.sep490.hdbhms.billingandpayment.infrastructure.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.billingandpayment.application.port.in.command.ReconcilePaymentCommand;
import com.sep490.hdbhms.billingandpayment.application.port.in.query.GetPaymentIntentQuery;
import com.sep490.hdbhms.billingandpayment.application.port.in.usecase.GetPaymentIntentUseCase;
import com.sep490.hdbhms.billingandpayment.application.port.in.usecase.ReconcilePaymentUseCase;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PaymentIntentStatus;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PaymentStatus;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.TransactionProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhook/vnpay")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VNPayIPNController {
    ObjectMapper objectMapper;
    VNPaySignatureVerifier vnPaySignatureVerifier;
    GetPaymentIntentUseCase getPaymentIntentUseCase;
    ReconcilePaymentUseCase reconcilePaymentUseCase;

    @GetMapping("/ipn")
    public Map<String, String> ipn(@RequestParam Map<String, String> params) {
        if (!vnPaySignatureVerifier.isValid(params)) {
            return Map.of("RspCode", "97", "Message", "Invalid signature");
        }

        String responseCode = params.get("vnp_ResponseCode");
        Long paymentIntentId = null;
        try {
            String txnRef = params.get("vnp_TxnRef");
            paymentIntentId = Long.parseLong(txnRef);
        } catch (NumberFormatException e) {
            return Map.of("RspCode", "97", "Message", "Invalid transaction reference");
        }
        PaymentIntent paymentIntent = getPaymentIntentUseCase.execute(new GetPaymentIntentQuery(paymentIntentId));
        if (paymentIntent.getStatus() == PaymentIntentStatus.SUCCEEDED) {
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        }

        var status = "00".equals(responseCode) ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        try {
            Long amount = Long.parseLong(params.get("vnp_Amount")) / 100;
            reconcilePaymentUseCase.execute(
                    ReconcilePaymentCommand.builder()
                            .paymentIntentId(paymentIntent.getId())
                            .provider(TransactionProvider.BANK)
                            .providerTransactionId(params.get("vnp_BankTranNo"))
                            .amount(amount)
                            .content(params.get("vnp_OrderInfo"))
                            .transactionTime(LocalDateTime.parse(params.get("vnp_PayDate"), DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))
                            .rawPayload(objectMapper.writeValueAsString(params))
                            .build()
            );
        } catch (NumberFormatException | JsonProcessingException e) {
            throw new RuntimeException(e.getMessage());
        }

        return Map.of("RspCode", "00", "Message", "Confirm Success");
    }
}
