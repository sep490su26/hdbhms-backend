package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositAgreementDetailsResponse {
    Long id;
    String depositCode;
    RoomResponse room;
    Long amount;
    LocalDate expectedMoveInDate;
    LocalDate expectedLeaseSignDate;
    LocalDate depositExpiresAt;
    DepositAgreementStatus status;
    LocalDateTime confirmedAt;
    Long contractFileId;
    String note;
    LocalDateTime createdAt;
}
