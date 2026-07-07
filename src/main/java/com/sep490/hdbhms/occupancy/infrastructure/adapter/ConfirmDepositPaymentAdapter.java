package com.sep490.hdbhms.occupancy.infrastructure.adapter;

import com.sep490.hdbhms.billingandpayment.application.port.out.PaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PaymentIntentStatus;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PaymentStatus;
import com.sep490.hdbhms.occupancy.application.port.out.ConfirmPaymentIntentPort;
import com.sep490.hdbhms.occupancy.application.port.out.CreateLeadOrAssignTenantPort;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfirmDepositPaymentAdapter implements ConfirmPaymentIntentPort {
    PaymentIntentRepository paymentIntentRepository;
    DepositAgreementRepository depositAgreementRepository;

    @Override
    public DepositAgreement execute(Long paymentIndentId, PaymentStatus paymentStatus) {
        PaymentIntent paymentIntent = paymentIntentRepository.findById(paymentIndentId)
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND));
        if (paymentIntent.getStatus() != PaymentIntentStatus.PENDING) {
            throw new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND);
        }
        DepositAgreement depositAgreement = depositAgreementRepository
                .findById(paymentIntent.getDepositAgreementId())
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND));
        if (paymentStatus != PaymentStatus.SUCCEEDED) {
            paymentIntent.failPayment();
        }
        paymentIntent.succeedPayment();
        paymentIntentRepository.save(paymentIntent);

        return depositAgreement;
    }
}
