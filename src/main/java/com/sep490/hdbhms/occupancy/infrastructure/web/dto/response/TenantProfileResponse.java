package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

public record TenantProfileResponse(
        @JsonProperty("tenant_profile_id")
        Long tenantProfileId,

        String status,

        PersonProfileDto person,

        @JsonProperty("identity_document")
        IdentityDocumentDto identityDocument,

        List<VehicleDto> vehicles,

        @JsonProperty("emergency_contacts")
        List<EmergencyContactDto> emergencyContacts
) {
    public record PersonProfileDto(
            @JsonProperty("full_name")
            String fullName,

            String phone,
            String email,

            @JsonProperty("permanent_address")
            String permanentAddress,

            @JsonProperty("portrait_url")
            String portraitUrl
    ) {
    }

    public record IdentityDocumentDto(
            @JsonProperty("doc_type")
            String docType,

            @JsonProperty("doc_number")
            String docNumber,

            @JsonProperty("issued_date")
            LocalDate issuedDate,

            @JsonProperty("issued_place")
            String issuedPlace,

            @JsonProperty("front_file_url")
            String frontFileUrl,

            @JsonProperty("back_file_url")
            String backFileUrl
    ) {
    }

    public record VehicleDto(
            Long id,

            @JsonProperty("vehicle_type")
            String vehicleType,

            @JsonProperty("license_plate")
            String licensePlate,

            @JsonProperty("image_url")
            String imageUrl
    ) {
    }

    public record EmergencyContactDto(
            @JsonProperty("full_name")
            String fullName,

            String relationship,
            String phone
    ) {
    }
}
