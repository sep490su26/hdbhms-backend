package com.sep490.hdbhms.billingandpayment.application.port.out;

import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;

public interface DepositBatchCompletionPort {
    void execute(Invoice invoice);
}
