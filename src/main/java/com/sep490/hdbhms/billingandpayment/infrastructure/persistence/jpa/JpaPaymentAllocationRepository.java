package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentAllocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPaymentAllocationRepository extends JpaRepository<PaymentAllocationEntity, Long> {
}
