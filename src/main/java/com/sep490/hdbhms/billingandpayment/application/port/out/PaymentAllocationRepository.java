package com.sep490.hdbhms.billingandpayment.application.port.out;

import com.sep490.hdbhms.billingandpayment.domain.model.PaymentAllocation;

import java.util.Optional;

public interface PaymentAllocationRepository {
    PaymentAllocation save(PaymentAllocation paymentAllocation);

    Optional<PaymentAllocation> findById(Long id);
}
