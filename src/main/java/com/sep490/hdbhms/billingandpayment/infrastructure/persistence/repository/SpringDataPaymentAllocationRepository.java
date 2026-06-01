package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.repository;

import com.sep490.hdbhms.billingandpayment.application.port.out.PaymentAllocationRepository;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentAllocation;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentAllocationRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.mapper.PaymentAllocationPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataPaymentAllocationRepository implements PaymentAllocationRepository {
    JpaPaymentAllocationRepository jpaPaymentAllocationRepository;
    PaymentAllocationPersistenceMapper paymentAllocationPersistenceMapper;

    @Override
    public PaymentAllocation save(PaymentAllocation paymentAllocation) {
        return paymentAllocationPersistenceMapper.toDomain(
                jpaPaymentAllocationRepository.save(
                        paymentAllocationPersistenceMapper.toEntity(
                                paymentAllocation
                        )
                )
        );
    }

    @Override
    public Optional<PaymentAllocation> findById(Long id) {
        return jpaPaymentAllocationRepository.findById(id)
                .map(paymentAllocationPersistenceMapper::toDomain);
    }
}
