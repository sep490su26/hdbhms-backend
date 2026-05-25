package com.sep490.hdbhms.billingandpayment.application.service;

import com.sep490.hdbhms.billingandpayment.application.port.in.query.GetPaymentIntentQuery;
import com.sep490.hdbhms.billingandpayment.application.port.in.usecase.GetPaymentIntentUseCase;
import com.sep490.hdbhms.billingandpayment.application.port.out.PaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetPaymentIntentService implements GetPaymentIntentUseCase {
    PaymentIntentRepository paymentIntentRepository;

    @Override
    public PaymentIntent execute(GetPaymentIntentQuery query) {
        return paymentIntentRepository.findById(query.paymentIntentId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid payment intent"));
    }
}
