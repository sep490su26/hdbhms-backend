package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentIntentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPaymentIntentRepository extends JpaRepository<PaymentIntentEntity, Long> {
}
