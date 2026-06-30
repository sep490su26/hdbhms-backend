package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.valueObjects.LeaseStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeaseContractDetailsResponse {
    Long id;
    String contractCode;
    RoomResponse room;
    LocalDate startDate;
    LocalDate endDate;
    LocalDate rentStartDate;
    Long monthlyRent;
    Integer paymentCycleMonths;
    Long depositAmount;
    LeaseStatus status;
    Long contractFileId;
    String contractFileName;
    String contractFileUrl;
    String tenantIntention;
    LocalDate expectedVacantDate;
    String roleInContract;
    Boolean isPrimary;
    Boolean canRecordIntention;
    Boolean canRenew;
    String canRenewBlockedReason;
    LocalDateTime signedAt;
    LocalDateTime createdAt;
}
