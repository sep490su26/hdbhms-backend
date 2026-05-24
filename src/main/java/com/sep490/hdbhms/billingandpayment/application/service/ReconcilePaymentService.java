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
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.TransactionStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .rawPayload(command.getRawPayload().getBytes())
                .build();
        paymentTransaction = paymentTransactionRepository.save(paymentTransaction);

        PaymentIntent paymentIntent = paymentIntentRepository.findById(command.getPaymentIntentId())
                .orElse(null);
        if (paymentIntent == null || paymentIntent.getStatus() != PaymentIntentStatus.PENDING) {
            return;
        }

        paymentIntent.succeedPayment();
        paymentIntent = paymentIntentRepository.save(paymentIntent);

        Long invoiceId = paymentIntent.getInvoiceId();
        if (invoiceId == null) {
            return;
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        if (
                invoice.getStatus() != InvoiceStatus.ISSUED
                        && invoice.getStatus() != InvoiceStatus.PARTIALLY_PAID
        ) {
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

        paymentTransaction.setMatched();
        paymentTransactionRepository.save(paymentTransaction);
        completeInvoiceUseCase.execute(invoice, paymentAllocation);
    }
}
