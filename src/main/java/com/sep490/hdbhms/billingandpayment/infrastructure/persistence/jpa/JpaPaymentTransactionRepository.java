package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.domain.valueObjects.TransactionProvider;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, Long> {
    boolean existsByProviderAndProviderTransactionId(TransactionProvider provider, String providerTransactionId);
}
