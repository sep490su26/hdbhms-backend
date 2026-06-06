package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.repository;

import com.sep490.hdbhms.billingandpayment.application.port.out.PaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.mapper.PaymentIntentPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataPaymentIntentRepository implements PaymentIntentRepository {
    JpaPaymentIntentRepository jpaPaymentIntentRepository;
    PaymentIntentPersistenceMapper paymentIntentPersistenceMapper;

    @Override
    public PaymentIntent save(PaymentIntent paymentIntent) {
        return paymentIntentPersistenceMapper.toDomain(
                jpaPaymentIntentRepository.save(
                        paymentIntentPersistenceMapper.toEntity(paymentIntent)
                )
        );
    }

    @Override
    public Optional<PaymentIntent> findById(Long id) {
        return jpaPaymentIntentRepository.findById(id)
                .map(paymentIntentPersistenceMapper::toDomain);
    }

    @Override
    public Optional<PaymentIntent> findByProviderOrderCode(String orderCode) {
        return jpaPaymentIntentRepository.findByProviderOrderCode(orderCode)
                .map(paymentIntentPersistenceMapper::toDomain);
    }
}
