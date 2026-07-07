package com.sep490.hdbhms.billingandpayment.application.service;

import com.sep490.hdbhms.billingandpayment.application.port.in.command.ReconcilePaymentCommand;
import com.sep490.hdbhms.billingandpayment.application.port.in.usecase.CompleteInvoiceUseCase;
import com.sep490.hdbhms.billingandpayment.application.port.in.usecase.ReconcilePaymentUseCase;
import com.sep490.hdbhms.billingandpayment.application.port.out.InvoiceRepository;
import com.sep490.hdbhms.billingandpayment.application.port.out.PaymentAllocationRepository;
import com.sep490.hdbhms.billingandpayment.application.port.out.PaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.application.port.out.PaymentTransactionRepository;
import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentAllocation;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentTransaction;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceType;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PaymentIntentStatus;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.TransactionStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReconcilePaymentService implements ReconcilePaymentUseCase {
    InvoiceRepository invoiceRepository;
    CompleteInvoiceUseCase completeInvoiceUseCase;
    PaymentIntentRepository paymentIntentRepository;
    PaymentAllocationRepository paymentAllocationRepository;
    PaymentTransactionRepository paymentTransactionRepository;

    @Override
    public void execute(ReconcilePaymentCommand command) {
        //Nếu có tồn tại 1 transaction giống provider và transaction id
        //thuộc về phía provider đó thì transaction đó đã tồn tại và không
        //cần giải quyết gì nữa
        if (
                paymentTransactionRepository.existByProviderAndProviderTransactionId(
                        command.getProvider(), command.getProviderTransactionId()
                )
        ) {
            replayPaidTransferDifferenceCompletion(command.getPaymentIntentId());
            return;
        }
        PaymentTransaction paymentTransaction = PaymentTransaction.builder()
                .provider(command.getProvider())
                .providerTransactionId(command.getProviderTransactionId())
                .amount(command.getAmount())
                .transactionTime(command.getTransactionTime())
                .payerName(command.getPayerName())
                .payerAccount(command.getPayerAccount())
                .content(command.getContent())
                .status(TransactionStatus.PENDING_RECONCILE)
                .rawPayload(valueOrEmpty(command.getRawPayload()).getBytes(StandardCharsets.UTF_8))
                .build();
        paymentTransaction = paymentTransactionRepository.save(paymentTransaction);

        PaymentIntent paymentIntent = paymentIntentRepository.findById(command.getPaymentIntentId())
                .orElse(null);
        if (paymentIntent == null || !canReconcile(paymentIntent)) {
            paymentTransaction.reject();
            paymentTransactionRepository.save(paymentTransaction);
            return;
        }

        if (isExpired(paymentIntent, command.getTransactionTime())) {
            if (paymentIntent.getDepositBatchId() != null
                    && reconcileLateBatchPayment(command, paymentIntent, paymentTransaction)) {
                return;
            }
            paymentIntent.expirePayment();
            paymentIntentRepository.save(paymentIntent);
            paymentTransaction.reject();
            paymentTransactionRepository.save(paymentTransaction);
            return;
        }

        Long invoiceId = paymentIntent.getInvoiceId();
        if (invoiceId == null) {
            paymentTransaction.reject();
            paymentTransactionRepository.save(paymentTransaction);
            return;
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        if (
                invoice.getStatus() != InvoiceStatus.ISSUED
                        && invoice.getStatus() != InvoiceStatus.PARTIALLY_PAID
        ) {
            paymentTransaction.reject();
            paymentTransactionRepository.save(paymentTransaction);
            return;
        }

        if (!isValidFullPayment(command, paymentIntent, invoice)) {
            paymentTransaction.reject();
            paymentTransactionRepository.save(paymentTransaction);
            return;
        }

        long amountToAllocate = command.getAmount();
        PaymentAllocation paymentAllocation = PaymentAllocation.allocate(
                paymentTransaction.getId(),
                invoiceId,
                amountToAllocate
        );
        paymentAllocation = paymentAllocationRepository.save(paymentAllocation);
        invoice.applyAmount(amountToAllocate);
        invoice = invoiceRepository.save(invoice);

        if (invoice.getStatus() != InvoiceStatus.PAID) {
            paymentTransaction.reject();
            paymentTransactionRepository.save(paymentTransaction);
            return;
        }

        paymentIntent.succeedPayment();
        paymentIntent = paymentIntentRepository.save(paymentIntent);

        paymentTransaction.setMatched();
        paymentTransactionRepository.save(paymentTransaction);
        completeInvoiceUseCase.execute(invoice, paymentAllocation);
    }

    private boolean isExpired(PaymentIntent paymentIntent, LocalDateTime transactionTime) {
        LocalDateTime effectivePaymentTime = transactionTime == null ? LocalDateTime.now() : transactionTime;
        return paymentIntent.getExpiresAt() != null
                && paymentIntent.getExpiresAt().isBefore(effectivePaymentTime);
    }

    private void replayPaidTransferDifferenceCompletion(Long paymentIntentId) {
        if (paymentIntentId == null) {
            return;
        }
        PaymentIntent paymentIntent = paymentIntentRepository.findById(paymentIntentId).orElse(null);
        if (paymentIntent == null || paymentIntent.getInvoiceId() == null) {
            return;
        }
        Invoice invoice = invoiceRepository.findById(paymentIntent.getInvoiceId()).orElse(null);
        if (invoice == null
                || invoice.getInvoiceType() != InvoiceType.TRANSFER_DIFFERENCE
                || invoice.getStatus() != InvoiceStatus.PAID) {
            return;
        }
        completeInvoiceUseCase.execute(invoice, null);
    }

    private boolean canReconcile(PaymentIntent paymentIntent) {
        return paymentIntent.getStatus() == PaymentIntentStatus.PENDING
                || paymentIntent.getStatus() == PaymentIntentStatus.EXPIRED;
    }

    private boolean reconcileLateBatchPayment(
            ReconcilePaymentCommand command,
            PaymentIntent paymentIntent,
            PaymentTransaction paymentTransaction
    ) {
        if (paymentIntent.getInvoiceId() == null) {
            return false;
        }
        Invoice invoice = invoiceRepository.findById(paymentIntent.getInvoiceId()).orElse(null);
        if (invoice == null
                || (invoice.getStatus() != InvoiceStatus.ISSUED
                && invoice.getStatus() != InvoiceStatus.PARTIALLY_PAID)
                || !isValidFullPayment(command, paymentIntent, invoice)) {
            return false;
        }

        PaymentAllocation paymentAllocation = paymentAllocationRepository.save(PaymentAllocation.allocate(
                paymentTransaction.getId(),
                invoice.getId(),
                command.getAmount()
        ));
        invoice.applyAmount(command.getAmount());
        invoice = invoiceRepository.save(invoice);
        paymentIntent.requireRefund();
        paymentIntentRepository.save(paymentIntent);
        paymentTransaction.setMatched();
        paymentTransactionRepository.save(paymentTransaction);
        completeInvoiceUseCase.execute(invoice, paymentAllocation);
        return true;
    }

    private boolean isValidFullPayment(
            ReconcilePaymentCommand command,
            PaymentIntent paymentIntent,
            Invoice invoice
    ) {
        if (command.getAmount() == null || command.getAmount() <= 0) {
            return false;
        }
        if (!Objects.equals(command.getAmount(), paymentIntent.getAmount())) {
            return false;
        }
        return Objects.equals(command.getAmount(), invoice.getRemainingAmount());
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
