package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import java.time.LocalDate;
import java.util.List;

public record TenantProfileManagementResponse(
        List<TenantProfileItem> items,
        List<RoomFilter> rooms,
        List<PropertyFilter> properties
) {
    public record TenantProfileItem(
            Long tenantId,
            Long personProfileId,
            Long contractId,
            String fullName,
            LocalDate dob,
            String gender,
            String phone,
            String email,
            String permanentAddress,
            String portraitUrl,
            IdentityDocument identityDocument,
            Long propertyId,
            String propertyName,
            Long roomId,
            String roomCode,
            Integer occupantCount,
            Integer maxOccupants,
            String role,
            LocalDate moveInDate,
            String residenceStatus,
            String profileStatus,
            String appStatus,
            List<Roommate> roommates,
            List<Vehicle> vehicles,
            List<EmergencyContact> emergencyContacts,
            ContractSummary contract
    ) {}

    public record IdentityDocument(
            String docType,
            String docNumber,
            LocalDate issuedDate,
            String issuedPlace,
            String frontUrl,
            String backUrl
    ) {}

    public record Roommate(
            Long tenantId,
            Long personProfileId,
            String fullName,
            LocalDate dob,
            String phone,
            String role
    ) {}

    public record Vehicle(Long id, String vehicleType, String licensePlate, String imageUrl) {}

    public record EmergencyContact(String fullName, String relationship, String phone) {}

    public record ContractSummary(
            Long id,
            String contractCode,
            LocalDate startDate,
            LocalDate endDate,
            String status
    ) {}

    public record RoomFilter(Long id, String roomCode) {}

    public record PropertyFilter(Long id, String propertyName) {}
}