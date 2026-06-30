package com.sep490.hdbhms.billingandpayment.application.service;

import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompleteInvoiceServiceTest {

    @Test
    void paidBatchDepositUsesBatchCompletionOnly() {
        AtomicInteger singleCalls = new AtomicInteger();
        AtomicInteger batchCalls = new AtomicInteger();
        CompleteInvoiceService service = new CompleteInvoiceService(
                invoice -> singleCalls.incrementAndGet(),
                invoice -> batchCalls.incrementAndGet()
        );

        service.execute(Invoice.builder()
                .invoiceType(InvoiceType.DEPOSIT)
                .status(InvoiceStatus.PAID)
                .depositBatchId(12L)
                .build(), null);

        assertEquals(0, singleCalls.get());
        assertEquals(1, batchCalls.get());
    }

    @Test
    void paidSingleDepositKeepsExistingCompletionFlow() {
        AtomicInteger singleCalls = new AtomicInteger();
        AtomicInteger batchCalls = new AtomicInteger();
        CompleteInvoiceService service = new CompleteInvoiceService(
                invoice -> singleCalls.incrementAndGet(),
                invoice -> batchCalls.incrementAndGet()
        );

        service.execute(Invoice.builder()
                .invoiceType(InvoiceType.DEPOSIT)
                .status(InvoiceStatus.PAID)
                .depositAgreementId(7L)
                .build(), null);

        assertEquals(1, singleCalls.get());
        assertEquals(0, batchCalls.get());
    }
}
