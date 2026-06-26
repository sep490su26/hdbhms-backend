package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdateTenantProfileRequest(
        @NotBlank
        String phone,

        @NotBlank
        String email,

        @JsonAlias("emergency_contacts")
        List<EmergencyContactDto> emergencyContacts,

        List<VehicleDto> vehicles
) {
    public record EmergencyContactDto(
            @JsonAlias("full_name")
            @NotBlank
            String fullName,

            @NotBlank
            String relationship,

            @NotBlank
            String phone
    ) {
    }

    public record VehicleDto(
            @JsonAlias("vehicle_type")
            String vehicleType,

            @JsonAlias("license_plate")
            @NotBlank
            String licensePlate,

            @JsonAlias("image_file_id")
            Long imageFileId
    ) {
    }
}
