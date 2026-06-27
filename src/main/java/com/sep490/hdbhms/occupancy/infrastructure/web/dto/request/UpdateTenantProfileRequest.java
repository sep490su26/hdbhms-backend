package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdateTenantProfileRequest(
        @NotBlank
        String phone,

        @NotBlank
        String email,

        @JsonProperty("emergency_contacts")
        List<EmergencyContactDto> emergencyContacts,

        List<VehicleDto> vehicles
) {
    public record EmergencyContactDto(
            @JsonProperty("full_name")
            @NotBlank
            String fullName,

            @NotBlank
            String relationship,

            @NotBlank
            String phone
    ) {
    }

    public record VehicleDto(
            @JsonProperty("vehicle_type")
            String vehicleType,

            @JsonProperty("license_plate")
            @NotBlank
            String licensePlate,

            @JsonProperty("image_file_id")
            Long imageFileId
    ) {
    }
}
