package com.sep490.hdbhms.billingandpayment.application.port.out;

import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceType;

import java.util.Optional;

public interface InvoiceRepository {
    Invoice save(Invoice invoice);

    Optional<Invoice> findById(Long id);

    Optional<Invoice> findFirstByLeastContractIdAndBillingPeriodAndInvoiceTypeAndStatusOrderByIdDesc(Long leaseContractId, String billingPeriod, InvoiceType invoiceType, InvoiceStatus invoiceStatus);
}
