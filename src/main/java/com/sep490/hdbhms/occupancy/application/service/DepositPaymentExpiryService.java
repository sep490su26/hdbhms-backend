package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.billingandpayment.application.port.out.PaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentStatus;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.application.port.out.EarlyCancelRoomHoldTaskPort;
import com.sep490.hdbhms.occupancy.application.port.out.RoomHoldRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomDepositFailureReason;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomHoldStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositPaymentExpiryService {
    PaymentIntentRepository paymentIntentRepository;
    DepositAgreementRepository depositAgreementRepository;
    RoomHoldRepository roomHoldRepository;
    RoomRepository roomRepository;
    EarlyCancelRoomHoldTaskPort earlyCancelRoomHoldTaskPort;
    RoomCommitmentChecker roomCommitmentChecker;
    RoomDepositLockService roomDepositLockService;

    public PaymentIntent expire(Long paymentIntentId) {
        PaymentIntent paymentIntent = paymentIntentRepository.findById(paymentIntentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay phien thanh toan."));
        if (paymentIntent.getDepositAgreementId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phien thanh toan khong thuoc dat coc phong.");
        }
        if (paymentIntent.getStatus() == PaymentIntentStatus.SUCCEEDED
                || paymentIntent.getStatus() == PaymentIntentStatus.REFUND_REQUIRED) {
            return paymentIntent;
        }

        LocalDateTime now = LocalDateTime.now();
        if (!isDue(paymentIntent.getExpiresAt(), now)) {
            return paymentIntent;
        }

        DepositAgreement depositAgreement = depositAgreementRepository.findById(paymentIntent.getDepositAgreementId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay thong tin dat coc."));
        if (depositAgreement.getStatus() == DepositAgreementStatus.PENDING_PAYMENT) {
            boolean expiredHold = expireRoomHold(depositAgreement.getRoomHoldId(), now);
            if (expiredHold) {
                roomDepositLockService.recordFailure(
                        depositAgreement.getRoomId(),
                        depositAgreement.getRoomHoldId(),
                        paymentIntent.getId(),
                        RoomDepositFailureReason.PAYMENT_EXPIRED
                );
            }
        }

        if (paymentIntent.getStatus() == PaymentIntentStatus.PENDING
                || paymentIntent.getStatus() == PaymentIntentStatus.CREATED) {
            paymentIntent.expirePayment();
            return paymentIntentRepository.save(paymentIntent);
        }
        return paymentIntent;
    }

    private boolean expireRoomHold(Long roomHoldId, LocalDateTime now) {
        if (roomHoldId == null) {
            return false;
        }
        RoomHold roomHold = roomHoldRepository.findById(roomHoldId).orElse(null);
        if (roomHold == null
                || (roomHold.getStatus() != RoomHoldStatus.ACTIVE
                && roomHold.getStatus() != RoomHoldStatus.PAYMENT_PROCESSING)
                || !isDue(roomHold.getExpiresAt(), now)) {
            return false;
        }

        roomHold.releaseOnAutoExpired();
        roomHoldRepository.save(roomHold);
        earlyCancelRoomHoldTaskPort.execute(roomHold.getId());
        roomRepository.updateRoomStatusIfCurrent(
                roomHold.getRoomId(),
                RoomStatus.ON_HOLD,
                roomStatusAfterHoldRelease(roomHold.getRoomId())
        );
        return true;
    }

    private RoomStatus roomStatusAfterHoldRelease(Long roomId) {
        return roomCommitmentChecker.findExpectedVacantDateForBooking(roomId).isPresent()
                ? RoomStatus.SOON_VACANT
                : RoomStatus.VACANT;
    }

    private boolean isDue(LocalDateTime expiresAt, LocalDateTime now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }
}
