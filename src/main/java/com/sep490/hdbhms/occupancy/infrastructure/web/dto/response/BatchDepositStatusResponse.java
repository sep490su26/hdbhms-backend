package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.DepositBatchStatus;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record BatchDepositStatusResponse(
        Long batchId,
        String batchCode,
        DepositBatchStatus status,
        PaymentIntentStatus paymentStatus,
        String message,
        List<RoomStatusInfo> rooms
) {
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record RoomStatusInfo(
            Long roomId,
            String roomCode,
            String status
    ) {
    }
}
