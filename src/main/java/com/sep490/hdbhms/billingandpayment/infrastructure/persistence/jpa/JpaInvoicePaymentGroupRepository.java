package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoicePaymentGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInvoicePaymentGroupRepository extends JpaRepository<InvoicePaymentGroupEntity, Long> {
}
