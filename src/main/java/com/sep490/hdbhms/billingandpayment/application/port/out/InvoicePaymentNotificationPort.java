package com.sep490.hdbhms.billingandpayment.application.port.out;

public interface InvoicePaymentNotificationPort {
    void execute(Long invoiceId, Long paymentAmount);
}
