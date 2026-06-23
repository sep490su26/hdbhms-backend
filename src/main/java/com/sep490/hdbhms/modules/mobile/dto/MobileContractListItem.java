package com.sep490.hdbhms.modules.mobile.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record MobileContractListItem(
        Long id,

        @JsonProperty("contract_code")
        String contractCode,

        @JsonProperty("room_code")
        String roomCode,

        @JsonProperty("signed_at")
        LocalDate signedAt,

        String status
) {
}
