package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.sep490.hdbhms.billingandpayment.application.port.in.command.ReconcilePaymentCommand;
import com.sep490.hdbhms.billingandpayment.application.port.in.usecase.ReconcilePaymentUseCase;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PaymentIntentStatus;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.TransactionProvider;
import com.sep490.hdbhms.billingandpayment.application.port.out.PaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.config.PayOSProperties;
import com.sep490.hdbhms.occupancy.application.service.BatchRoomUnavailableException;
import com.sep490.hdbhms.occupancy.application.service.BatchDepositRequestException;
import com.sep490.hdbhms.occupancy.application.service.DepositBatchCheckoutService;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.BatchDepositCheckoutRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.BatchDepositCheckoutResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.BatchDepositStatusResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MultipartFile;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.v2.paymentRequests.Transaction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/deposits")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PublicDepositBatchController {
    DepositBatchCheckoutService depositBatchCheckoutService;
    PaymentIntentRepository paymentIntentRepository;
    ReconcilePaymentUseCase reconcilePaymentUseCase;
    PayOSProperties payOSProperties;
    ObjectMapper objectMapper;

    @PostMapping("/batch-checkout")
    public ResponseEntity<?> checkout(
            @Valid @RequestPart("metadata") BatchDepositCheckoutRequest request,
            @RequestPart("frontIdCardFile") MultipartFile frontIdCardFile,
            @RequestPart("backIdCardFile") MultipartFile backIdCardFile,
            @RequestPart("portraitFile") MultipartFile portraitFile
    ) {
        try {
            BatchDepositCheckoutResponse response = depositBatchCheckoutService.checkout(
                    request,
                    frontIdCardFile,
                    backIdCardFile,
                    portraitFile
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<BatchDepositCheckoutResponse>builder().data(response).build());
        } catch (BatchRoomUnavailableException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new BatchRoomUnavailableResponse(
                    "BATCH_ROOM_UNAVAILABLE",
                    ex.getMessage(),
                    ex.getUnavailableRooms(),
                    ex.getAvailableRooms()
            ));
        } catch (BatchDepositRequestException ex) {
            return ResponseEntity.status(ex.getStatus()).body(new BatchErrorResponse(
                    ex.getCode(),
                    ex.getMessage(),
                    null
            ));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BatchErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(new BatchErrorResponse(
                "VALIDATION_ERROR",
                "Dữ liệu không hợp lệ.",
                fieldErrors
        ));
    }

    @GetMapping("/batches/{batchId}/status")
    public ApiResponse<BatchDepositStatusResponse> getStatus(@PathVariable Long batchId) {
        syncPayOSPayment(batchId);
        return ApiResponse.<BatchDepositStatusResponse>builder()
                .data(depositBatchCheckoutService.getStatus(batchId))
                .build();
    }

    @PostMapping("/batches/{batchId}/cancel")
    public ApiResponse<BatchDepositStatusResponse> cancel(@PathVariable Long batchId) {
        return ApiResponse.<BatchDepositStatusResponse>builder()
                .data(depositBatchCheckoutService.cancel(batchId))
                .build();
    }

    private void syncPayOSPayment(Long batchId) {
        BatchDepositStatusResponse status = depositBatchCheckoutService.getStatus(batchId);
        if (status.paymentStatus() != PaymentIntentStatus.PENDING) {
            return;
        }
        PaymentIntent paymentIntent = paymentIntentRepository.findById(
                        depositBatchCheckoutService.getPaymentIntentId(batchId)
                )
                .orElse(null);
        if (paymentIntent == null || paymentIntent.getProvider() != PaymentIntentProvider.PAYOS) {
            return;
        }
        try {
            PaymentLink paymentLink = payOSProperties.payOS().paymentRequests()
                    .get(paymentIntent.getProviderOrderCode());
            if (paymentLink == null || paymentLink.getStatus() != PaymentLinkStatus.PAID) {
                return;
            }
            Transaction transaction = paymentLink.getTransactions() == null
                    || paymentLink.getTransactions().isEmpty()
                    ? null
                    : paymentLink.getTransactions().getFirst();
            Long amount = transaction != null && transaction.getAmount() != null
                    ? transaction.getAmount()
                    : paymentLink.getAmountPaid();
            reconcilePaymentUseCase.execute(ReconcilePaymentCommand.builder()
                    .paymentIntentId(paymentIntent.getId())
                    .provider(TransactionProvider.PAYOS)
                    .providerTransactionId(resolveProviderTransactionId(paymentLink, transaction))
                    .amount(amount)
                    .content(transaction == null ? paymentIntent.getPaymentContent() : transaction.getDescription())
                    .payerName(transaction == null ? null : transaction.getCounterAccountName())
                    .payerAccount(transaction == null ? null : transaction.getCounterAccountNumber())
                    .transactionTime(parseTransactionDateTime(
                            transaction == null ? null : transaction.getTransactionDateTime()
                    ))
                    .rawPayload(objectMapper.writeValueAsString(paymentLink))
                    .build());
        } catch (Exception ignored) {
            // Polling is a fallback; webhook remains the primary confirmation path.
        }
    }

    private String resolveProviderTransactionId(PaymentLink paymentLink, Transaction transaction) {
        if (transaction != null && transaction.getReference() != null && !transaction.getReference().isBlank()) {
            return transaction.getReference();
        }
        return "PAYOS-BATCH-POLL-" + paymentLink.getOrderCode() + "-" + paymentLink.getId();
    }

    private LocalDateTime parseTransactionDateTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        )) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (RuntimeException ignored) {
            }
        }
        return LocalDateTime.now();
    }

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    private record BatchRoomUnavailableResponse(
            String code,
            String message,
            List<BatchRoomUnavailableException.UnavailableRoom> unavailableRooms,
            List<BatchRoomUnavailableException.AvailableRoom> availableRooms
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    private record BatchErrorResponse(
            String code,
            String message,
            Map<String, String> fieldErrors
    ) {
    }
}
