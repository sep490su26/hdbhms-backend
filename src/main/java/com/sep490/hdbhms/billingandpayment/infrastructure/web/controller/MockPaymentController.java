package com.sep490.hdbhms.billingandpayment.infrastructure.web.controller;

import com.sep490.hdbhms.billingandpayment.application.port.in.command.ReconcilePaymentCommand;
import com.sep490.hdbhms.billingandpayment.application.port.in.usecase.ReconcilePaymentUseCase;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.TransactionProvider;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mock/payment")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MockPaymentController {
    ReconcilePaymentUseCase reconcilePaymentUseCase;

    @PostMapping
    public ApiResponse<Void> mockPayment() {
//        Long amount = Long.parseLong(params.get("vnp_Amount")) / 100;
        reconcilePaymentUseCase.execute(
                ReconcilePaymentCommand.builder()
                        .paymentIntentId(8L)
                        .provider(TransactionProvider.BANK)
                        .providerTransactionId("")
                        .amount(1L)
                        .content("")
                        .transactionTime(LocalDateTime.now())
                        .rawPayload("")
                        .build()
        );
        return ApiResponse.<Void>builder().build();
    }
}
