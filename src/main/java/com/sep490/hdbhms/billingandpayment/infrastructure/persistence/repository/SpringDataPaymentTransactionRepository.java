package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.repository;

import com.sep490.hdbhms.billingandpayment.application.port.out.PaymentTransactionRepository;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentTransaction;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.TransactionProvider;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentTransactionRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.mapper.PaymentTransactionPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataPaymentTransactionRepository implements PaymentTransactionRepository {
    JpaPaymentTransactionRepository jpaPaymentTransactionRepository;
    PaymentTransactionPersistenceMapper paymentTransactionPersistenceMapper;

    @Override
    public PaymentTransaction save(PaymentTransaction paymentTransaction) {
        return paymentTransactionPersistenceMapper.toDomain(
                jpaPaymentTransactionRepository.save(
                        paymentTransactionPersistenceMapper.toEntity(
                                paymentTransaction
                        )
                )
        );
    }

    @Override
    public Optional<PaymentTransaction> findById(Long id) {
        return jpaPaymentTransactionRepository.findById(id)
                .map(paymentTransactionPersistenceMapper::toDomain);
    }

    @Override
    public boolean existByProviderAndProviderTransactionId(
            TransactionProvider provider,
            String providerTransactionId
    ) {
        return jpaPaymentTransactionRepository
                .existsByProviderAndProviderTransactionId(
                        provider,
                        providerTransactionId
                );
    }
}
