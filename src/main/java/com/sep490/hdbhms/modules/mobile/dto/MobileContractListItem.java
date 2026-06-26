package com.sep490.hdbhms.modules.mobile.dto;

import java.time.LocalDate;

public record MobileContractListItem(
        Long id,

        String contractCode,

        String roomCode,

        LocalDate signedAt,

        String status
) {
}
