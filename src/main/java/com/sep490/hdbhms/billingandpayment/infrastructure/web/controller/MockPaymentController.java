package com.sep490.hdbhms.billingandpayment.infrastructure.web.controller;

import com.sep490.hdbhms.billingandpayment.application.port.in.command.ReconcilePaymentCommand;
import com.sep490.hdbhms.billingandpayment.application.port.in.usecase.ReconcilePaymentUseCase;
import com.sep490.hdbhms.billingandpayment.application.port.out.InvoiceRepository;
import com.sep490.hdbhms.billingandpayment.application.port.out.PaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.TransactionProvider;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@Profile({"dev", "test", "local"})
@ConditionalOnProperty(name = "app.mock-payment.enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/api/v1/mock")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MockPaymentController {
    ReconcilePaymentUseCase reconcilePaymentUseCase;
    PaymentIntentRepository paymentIntentRepository;
    InvoiceRepository invoiceRepository;

    @PostMapping("/payments/{paymentIntentId}/success")
    public ApiResponse<Void> mockPaymentSuccess(
            @PathVariable Long paymentIntentId,
            @RequestBody(required = false) MockPaymentRequest request
    ) {
        Long amount = resolveAmount(paymentIntentId, request);
        reconcile(paymentIntentId, amount);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/payment")
    public ApiResponse<Void> mockPayment(@RequestBody(required = false) MockPaymentRequest request) {
        if (request == null || request.getPaymentIntentId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thiếu paymentIntentId. Hãy gọi POST /api/v1/mock/payments/{paymentIntentId}/success hoặc truyền paymentIntentId trong body."
            );
        }

        Long amount = resolveAmount(request.getPaymentIntentId(), request);
        reconcile(request.getPaymentIntentId(), amount);
        return ApiResponse.<Void>builder().build();
    }

    private Long resolveAmount(Long paymentIntentId, MockPaymentRequest request) {
        PaymentIntent paymentIntent = paymentIntentRepository.findById(paymentIntentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy payment intent: " + paymentIntentId
                ));

        if (request != null && request.getAmount() != null) {
            if (request.getAmount() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền thanh toán phải lớn hơn 0.");
            }
            return request.getAmount();
        }

        if (paymentIntent.getInvoiceId() == null) {
            if (paymentIntent.getAmount() == null || paymentIntent.getAmount() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment intent không có số tiền hợp lệ.");
            }
            return paymentIntent.getAmount();
        }

        Invoice invoice = invoiceRepository.findById(paymentIntent.getInvoiceId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy invoice của payment intent: " + paymentIntentId
                ));
        Long remainingAmount = invoice.getRemainingAmount();
        if (remainingAmount != null && remainingAmount > 0) {
            return remainingAmount;
        }
        if (paymentIntent.getAmount() != null && paymentIntent.getAmount() > 0) {
            return paymentIntent.getAmount();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không xác định được số tiền cần thanh toán.");
    }

    private void reconcile(Long paymentIntentId, Long amount) {
        String providerTransactionId = "MOCK-" + paymentIntentId + "-" + UUID.randomUUID();
        reconcilePaymentUseCase.execute(
                ReconcilePaymentCommand.builder()
                        .paymentIntentId(paymentIntentId)
                        .provider(TransactionProvider.BANK)
                        .providerTransactionId(providerTransactionId)
                        .amount(amount)
                        .content("Mock payment success for payment intent " + paymentIntentId)
                        .transactionTime(LocalDateTime.now())
                        .rawPayload("{\"source\":\"mock-payment\",\"orderCode\":" + paymentIntentId + ",\"amount\":" + amount + "}")
                        .build()
        );
    }

    @Data
    static class MockPaymentRequest {
        Long paymentIntentId;
        Long amount;
    }
}
