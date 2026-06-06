package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentIntentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaPaymentIntentRepository extends JpaRepository<PaymentIntentEntity, Long> {
    Optional<PaymentIntentEntity> findByProviderOrderCode(String orderCode);
}
