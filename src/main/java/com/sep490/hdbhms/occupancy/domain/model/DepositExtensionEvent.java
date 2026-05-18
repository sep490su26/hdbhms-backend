package com.sep490.hdbhms.occupancy.domain.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositExtensionEvent {
    Long id;
    Long depositAgreementId;
    LocalDate oldExpectedMoveInDate;
    LocalDate newExpectedMoveInDate;
    LocalDate oldExpiresAt;
    LocalDate newExpiresAt;
    String reason;
    Long approvedById;
    LocalDateTime approvedAt;
}
