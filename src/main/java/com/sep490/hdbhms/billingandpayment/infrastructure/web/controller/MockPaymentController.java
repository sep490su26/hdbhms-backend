package com.sep490.hdbhms.billingandpayment.infrastructure.web.controller;

import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;

import com.sep490.hdbhms.billingandpayment.application.port.in.command.ReconcilePaymentCommand;
import com.sep490.hdbhms.billingandpayment.application.port.in.usecase.ReconcilePaymentUseCase;
import com.sep490.hdbhms.billingandpayment.application.port.out.InvoiceRepository;
import com.sep490.hdbhms.billingandpayment.application.port.out.PaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.TransactionProvider;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.shared.types.dto.response.ApiResponse;
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
import org.springframework.security.core.context.SecurityContextHolder;

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
    JpaInvoiceRepository jpaInvoiceRepository;

    @PostMapping("/payments/{paymentIntentId}/success")
    public ApiResponse<Void> mockPaymentSuccess(
            @PathVariable Long paymentIntentId,
            @RequestBody(required = false) MockPaymentRequest request
    ) {
        assertCanPayInvoice(paymentIntentId);
        Long amount = resolveAmount(paymentIntentId, request);
        reconcile(paymentIntentId, amount);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/payment")
    public ApiResponse<Void> mockPayment(@RequestBody(required = false) MockPaymentRequest request) {
        if (request == null || request.getPaymentIntentId() == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        assertCanPayInvoice(request.getPaymentIntentId());
        Long amount = resolveAmount(request.getPaymentIntentId(), request);
        reconcile(request.getPaymentIntentId(), amount);
        return ApiResponse.<Void>builder().build();
    }

    private Long resolveAmount(Long paymentIntentId, MockPaymentRequest request) {
        PaymentIntent paymentIntent = paymentIntentRepository.findById(paymentIntentId)
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));

        if (request != null && request.getAmount() != null) {
            if (request.getAmount() <= 0) {
                throw new AppException(ApiErrorCode.INVALID_REQUEST);
            }
            return request.getAmount();
        }

        if (paymentIntent.getInvoiceId() == null) {
            if (paymentIntent.getAmount() == null || paymentIntent.getAmount() <= 0) {
                throw new AppException(ApiErrorCode.INVALID_REQUEST);
            }
            return paymentIntent.getAmount();
        }

        Invoice invoice = invoiceRepository.findById(paymentIntent.getInvoiceId())
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        Long remainingAmount = invoice.getRemainingAmount();
        if (remainingAmount != null && remainingAmount > 0) {
            return remainingAmount;
        }
        if (paymentIntent.getAmount() != null && paymentIntent.getAmount() > 0) {
            return paymentIntent.getAmount();
        }
        throw new AppException(ApiErrorCode.INVALID_REQUEST);
    }

    private void assertCanPayInvoice(Long paymentIntentId) {
        PaymentIntent paymentIntent = paymentIntentRepository.findById(paymentIntentId)
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        if (paymentIntent.getInvoiceId() == null) {
            return;
        }
        Invoice invoice = invoiceRepository.findById(paymentIntent.getInvoiceId())
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        if (invoice.getLeaseContractId() == null) {
            return;
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = authentication != null
                && authentication.getPrincipal() instanceof UserPrincipal principal
                ? principal.getId()
                : null;
        if (currentUserId == null
                || !jpaInvoiceRepository.existsByIdAndLeastContract_PrimaryTenantProfile_User_Id(
                paymentIntent.getInvoiceId(), currentUserId)) {
            throw new AppException(ApiErrorCode.FORBIDDEN_OPERATION);
        }
    }

    private void reconcile(Long paymentIntentId, Long amount) {
        String providerTransactionId = "MOCK-" + paymentIntentId + "-" + UUID.randomUUID();
        reconcilePaymentUseCase.execute(
                ReconcilePaymentCommand.builder()
                        .paymentIntentId(paymentIntentId)
                        .provider(TransactionProvider.BANK)
                        .providerTransactionId(providerTransactionId)
                        .amount(amount)
                        .content("Thanh toán mẫu thành công cho lệnh thanh toán " + paymentIntentId)
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
