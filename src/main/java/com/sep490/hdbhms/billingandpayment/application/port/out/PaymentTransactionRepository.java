package com.sep490.hdbhms.billingandpayment.application.port.out;

import com.sep490.hdbhms.billingandpayment.domain.model.PaymentTransaction;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.TransactionProvider;

import java.util.Optional;

public interface PaymentTransactionRepository {
    PaymentTransaction save(PaymentTransaction paymentTransaction);

    Optional<PaymentTransaction> findById(Long id);

    boolean existByProviderAndProviderTransactionId(TransactionProvider provider, String providerTransactionId);
}
