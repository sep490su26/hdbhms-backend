package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import java.time.LocalDate;
import java.util.List;

public record TenantProfileResponse(
        Long tenantProfileId,

        String status,

        PersonProfileDto person,
        IdentityDocumentDto identityDocument,

        List<VehicleDto> vehicles,
        List<EmergencyContactDto> emergencyContacts
) {
    public record PersonProfileDto(
            String fullName,
            
            LocalDate dob,

            String phone,
            String email,
            String permanentAddress,
            String portraitUrl
    ) {
    }

    public record IdentityDocumentDto(
            String docType,
            String docNumber,
            LocalDate issuedDate,
            String issuedPlace,
            String frontFileUrl,
            String backFileUrl
    ) {
    }

    public record VehicleDto(
            Long id,
            String vehicleType,
            String licensePlate,
            String imageUrl
    ) {
    }

    public record EmergencyContactDto(
            String fullName,

            String relationship,
            String phone
    ) {
    }
}
