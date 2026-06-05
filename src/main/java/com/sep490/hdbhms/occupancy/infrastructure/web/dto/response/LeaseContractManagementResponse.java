package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
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
public class LeaseContractManagementResponse {
    String sourceType;
    Long leaseContractId;
    Long depositAgreementId;
    String code;
    String depositCode;
    String contractCode;

    Long propertyId;
    String propertyName;
    String propertyAddress;

    Long roomId;
    String roomCode;
    RoomStatus roomStatus;

    Long primaryTenantProfileId;
    String customerName;
    String phone;
    String email;

    LocalDate expectedLeaseSignDate;
    LocalDate expectedMoveInDate;
    LocalDate startDate;
    LocalDate endDate;
    Long monthlyRent;
    Integer paymentCycleMonths;
    Long depositAmount;

    LeaseStatus contractStatus;
    DepositAgreementStatus depositStatus;
    String workflowStatus;

    Long contractFileId;
    String contractFileName;
    LocalDateTime contractFileUploadedAt;
    LocalDateTime signedAt;
    LocalDateTime createdAt;

    Boolean accountProvisioned;
    Boolean emailAvailable;
}
