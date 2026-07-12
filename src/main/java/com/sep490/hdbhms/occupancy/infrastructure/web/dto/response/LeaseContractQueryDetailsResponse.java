package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.TenantAccountProvisioningStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantRole;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LeaseContractQueryDetailsResponse(
        Long contractId,
        String contractCode,
        Long depositAgreementId,
        Long depositSignedFileId,
        RoomInfo room,
        PropertyInfo property,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate rentStartDate,
        Long monthlyRent,
        Integer paymentCycleMonths,
        Long depositAmount,
        LeaseStatus status,
        LocalDateTime signedAt,
        Long previousContractId,
        String previousContractCode,
        Long renewedContractId,
        String renewedContractCode,
        String tenantIntention,
        LocalDate expectedVacantDate,
        LocalDateTime intentionRecordedAt,
        Long transferRequestId,
        String transferRequestCode,
        String transferStatus,
        LocalDate transferRequestedDate,
        String transferContractRole,
        Boolean transferActivationLocked,
        boolean canRenew,
        boolean canLiquidate,
        boolean canSendAccount,
        String accountProvisioningStatus,
        ContractFileInfo contractFile,
        TenantProfileInfo primaryTenant,
        List<OccupantInfo> occupants,
        List<EventInfo> events
) {
    public record RoomInfo(Long id, String roomCode, String name) {
    }

    public record PropertyInfo(Long id, String name, String address) {
    }

    public record TenantProfileInfo(
            Long id,
            String fullName,
            String phone,
            String email,
            LocalDate dob,
            String permanentAddress,
            String citizenId,
            LocalDate identityIssuedDate,
            String identityIssuedPlace
    ) {
    }

    public record ContractFileInfo(Long id, String fileName) {
    }

    public record OccupantInfo(
            Long tenantProfileId,
            String fullName,
            String phone,
            String email,
            LocalDate dob,
            String permanentAddress,
            String citizenId,
            LocalDate identityIssuedDate,
            String identityIssuedPlace,
            OccupantRole occupantRole,
            LocalDate moveInDate,
            LocalDate moveOutDate,
            OccupantStatus status,
            TenantAccountProvisioningStatus accountStatus,
            LocalDateTime accountSentAt,
            LocalDateTime lastLoginAt,
            Boolean mustChangePassword
    ) {
    }

    public record EventInfo(
            Long id,
            String eventType,
            String eventData,
            LocalDateTime createdAt
    ) {
    }
}
