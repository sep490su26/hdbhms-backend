package com.sep490.hdbhms.occupancy.infrastructure.adapter;

import com.sep490.hdbhms.billingandpayment.application.port.out.PaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentStatus;
import com.sep490.hdbhms.occupancy.application.port.out.ConfirmPaymentIntentPort;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.application.service.RoomDepositLockService;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomDepositFailureReason;
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
    RoomDepositLockService roomDepositLockService;

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
            paymentIntentRepository.save(paymentIntent);
            roomDepositLockService.recordFailure(
                    depositAgreement.getRoomId(),
                    depositAgreement.getRoomHoldId(),
                    paymentIntent.getId(),
                    RoomDepositFailureReason.PAYMENT_FAILED
            );
            return depositAgreement;
        }
        paymentIntent.succeedPayment();
        paymentIntentRepository.save(paymentIntent);

        return depositAgreement;
    }
}
