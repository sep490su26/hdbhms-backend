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
    String roomCode;
    String propertyName;
    String propertyAddress;
    String depositorFullName;
    String depositorPhone;
    String depositorEmail;
    Long amount;
    LocalDate expectedMoveInDate;
    LocalDate expectedLeaseSignDate;
    LocalDate depositExpiresAt;
    DepositAgreementStatus status;
    LocalDateTime confirmedAt;
    Long contractFileId;
    String contractDownloadUrl;
    Long idFrontFileId;
    String idFrontFileUrl;
    Long idBackFileId;
    String idBackFileUrl;
    Long portraitFileId;
    String portraitFileUrl;
    String note;
    LocalDateTime createdAt;
}
