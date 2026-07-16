package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record BatchDepositCheckoutResponse(
        Long batchId,
        String batchCode,
        Long paymentIntentId,
        Long totalAmount,
        String currency,
        String checkoutUrl,
        String qrCode,
        String qrPayload,
        String providerOrderCode,
        String paymentLinkId,
        String bankBin,
        String bankShortName,
        String accountNumber,
        String accountName,
        String transferDescription,
        LocalDateTime expiresAt,
        List<RoomInfo> rooms,
        String accessToken
) {
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record RoomInfo(
            Long roomId,
            String roomCode,
            Long depositAmount,
            LocalDateTime holdExpiresAt
    ) {
    }
}
