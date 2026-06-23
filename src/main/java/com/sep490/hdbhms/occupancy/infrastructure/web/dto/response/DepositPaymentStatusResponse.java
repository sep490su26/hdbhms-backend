package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DepositPaymentStatusResponse(
        Long paymentIntentId,
        PaymentIntentStatus status,
        DepositAgreementStatus depositStatus,
        RoomStatus roomStatus,
        LocalDateTime expiresAt,
        LocalDateTime paidAt,
        String message
) {
}
