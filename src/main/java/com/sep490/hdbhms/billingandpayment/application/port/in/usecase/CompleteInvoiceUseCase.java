package com.sep490.hdbhms.billingandpayment.application.port.in.usecase;

import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentAllocation;

public interface CompleteInvoiceUseCase {
    void execute(Invoice invoice, PaymentAllocation paymentAllocation);
}
