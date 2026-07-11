package com.sep490.hdbhms.billingandpayment.application.port.out;

import com.sep490.hdbhms.billingandpayment.domain.model.InvoiceLine;

import java.util.Optional;

public interface InvoiceLineRepository {
    InvoiceLine save(InvoiceLine invoiceLine);

    Optional<InvoiceLine> findById(Long id);

    Optional<InvoiceLine> findByInvoiceId(Long invoiceId);
}
