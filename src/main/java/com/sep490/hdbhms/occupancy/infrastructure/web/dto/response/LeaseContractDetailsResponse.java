package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.property.infrastructure.web.dto.response.RoomResponse;

import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    Long signedFileId;
    String signedFileName;
    String signedFileUrl;
    String tenantIntention;
    LocalDate expectedVacantDate;
    String roleInContract;
    Long currentTenantProfileId;
    List<LeaseContractQueryDetailsResponse.OccupantInfo> occupants;
    Boolean isPrimary;
    Boolean canRecordIntention;
    Boolean canRecordOccupantIntention;
    String occupantIntention;
    String occupantIntentionNote;
    LocalDateTime occupantIntentionRecordedAt;
    Boolean canRenew;
    String canRenewBlockedReason;
    Boolean canLiquidate;
    String canLiquidateBlockedReason;
    Boolean canAddCoOccupant;
    String canAddCoOccupantBlockedReason;
    Boolean canChangeRoom;
    String canChangeRoomBlockedReason;
    LocalDateTime signedAt;
    LocalDateTime createdAt;
}
