package com.sep490.hdbhms.billingandpayment.domain.model;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.shared.exception.AppException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvoicePaymentPolicyTest {

    @Test
    void fullPaymentMovesIssuedInvoiceToPaid() {
        Invoice invoice = invoice(InvoiceStatus.ISSUED);

        invoice.applyAmount(1_000L);

        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(1_000L, invoice.getPaidAmount());
        assertEquals(0L, invoice.getRemainingAmount());
    }

    @Test
    void partialPaymentIsRejected() {
        Invoice invoice = invoice(InvoiceStatus.ISSUED);

        assertThrows(AppException.class, () -> invoice.applyAmount(500L));
    }

    @Test
    void overdueInvoiceCanBePaidInFull() {
        Invoice invoice = invoice(InvoiceStatus.OVERDUE);

        invoice.applyAmount(1_000L);

        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
    }

    @Test
    void paidInvoiceCannotBePaidAgain() {
        Invoice invoice = invoice(InvoiceStatus.PAID);

        assertThrows(AppException.class, () -> invoice.applyAmount(1_000L));
    }

    private Invoice invoice(InvoiceStatus status) {
        return Invoice.builder()
                .status(status)
                .totalAmount(1_000L)
                .paidAmount(0L)
                .remainingAmount(1_000L)
                .build();
    }
}
