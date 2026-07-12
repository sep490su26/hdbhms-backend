package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdateTenantProfileRequest(
        @NotBlank
        String phone,

        @NotBlank
        String email,

        List<EmergencyContactDto> emergencyContacts,

        List<VehicleDto> vehicles
) {
    public record EmergencyContactDto(
            @NotBlank
            String fullName,

            @NotBlank
            String relationship,

            @NotBlank
            String phone
    ) {
    }

    public record VehicleDto(
            String vehicleType,

            @NotBlank
            String licensePlate,

            Long imageFileId
    ) {
    }
}
