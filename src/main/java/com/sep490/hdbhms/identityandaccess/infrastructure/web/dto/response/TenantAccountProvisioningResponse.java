package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.TenantAccountProvisioningStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.OccupantStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantAccountProvisioningResponse {
    Long contractId;
    String contractCode;
    LeaseStatus contractStatus;
    LocalDate startDate;
    LocalDate endDate;
    LocalDateTime signedAt;
    Long propertyId;
    String propertyName;
    Long roomId;
    String roomCode;
    RoomStatus roomStatus;
    Long occupantId;
    Long profileId;
    String roomRole;
    OccupantStatus occupantStatus;
    Integer roomOccupantCount;
    Integer roomMaxOccupants;
    Long userId;
    String fullName;
    String phone;
    String email;
    String recipientEmail;
    Role role;
    AccountStatus accountStatus;
    Boolean mustChangePassword;
    LocalDateTime lastLoginAt;
    LocalDateTime accountCreatedAt;
    Boolean accountProvisioned;
    Boolean emailAvailable;
    TenantAccountProvisioningStatus provisioningStatus;
    LocalDateTime sentAt;
    LocalDateTime failedAt;
    String failureReason;
    String disabledReason;
    Long disabledBy;
    LocalDateTime disabledAt;
    Integer attemptCount;
    LocalDateTime lastAttemptAt;
    String profileStatus;
    Boolean missingIdentity;
    Boolean missingPortrait;
    Boolean missingEmergencyContact;
    String message;
}
