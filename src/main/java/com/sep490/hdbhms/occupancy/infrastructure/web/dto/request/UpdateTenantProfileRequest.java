package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import java.util.List;

public record UpdateTenantProfileRequest(
        @NotBlank(message = "Vui lòng nhập số điện thoại người liên hệ")
        String phone,

        @NotBlank(message = "Vui lòng nhập email")
        @Email(message = "Địa chỉ email sai định dạng")
        String email,

        List<EmergencyContactDto> emergencyContacts,

        List<VehicleDto> vehicles
) {
    public record EmergencyContactDto(
            @NotBlank(message = "Vui lòng nhập tên người liên hệ khẩn cấp")
            String fullName,

            @NotBlank(message = "Vui lòng nhập mối quan hệ")
            String relationship,

            @NotBlank(message = "Vui lòng nhập số điện thoại người liên hệ")
            String phone
    ) {
    }

    public record VehicleDto(
            String vehicleType,

            @NotBlank(message = "Vui lòng nhập biển số xe")
            String licensePlate,

            Long imageFileId
    ) {
    }
}
