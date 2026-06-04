package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
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
    Long profileId;
    Long userId;
    String fullName;
    String phone;
    String email;
    Role role;
    AccountStatus accountStatus;
    Boolean mustChangePassword;
    LocalDateTime lastLoginAt;
    LocalDateTime accountCreatedAt;
    Boolean accountProvisioned;
    Boolean emailAvailable;
    String message;
}
