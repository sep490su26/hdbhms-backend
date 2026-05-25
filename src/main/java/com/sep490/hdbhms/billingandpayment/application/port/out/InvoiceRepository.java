package com.sep490.hdbhms.billingandpayment.application.port.out;

import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;

import java.util.Optional;

public interface InvoiceRepository {
    Invoice save(Invoice invoice);

    Optional<Invoice> findById(Long id);
}
