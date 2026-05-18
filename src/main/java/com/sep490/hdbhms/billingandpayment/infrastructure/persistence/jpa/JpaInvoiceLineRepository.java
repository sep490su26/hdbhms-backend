package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInvoiceLineRepository extends JpaRepository<InvoiceLineEntity, Long> {
}
