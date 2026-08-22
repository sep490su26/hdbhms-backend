package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record UpdateTenantProfileRequest(
        @NotBlank(message = "Vui lòng nhập số điện thoại người liên hệ")
        @Pattern(
                regexp = "^(0\\d{9}|\\+84\\d{9})$",
                message = "Số điện thoại không hợp lệ"
        )
        String phone,

        @Pattern(
                regexp = "^$|^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
                message = "Địa chỉ email sai định dạng"
        )
        String email,

        List<@Valid EmergencyContactDto> emergencyContacts,

        List<@Valid VehicleDto> vehicles
) {
    public record EmergencyContactDto(
            @NotBlank(message = "Vui lòng nhập tên người liên hệ khẩn cấp")
            String fullName,

            @NotBlank(message = "Vui lòng nhập mối quan hệ")
            String relationship,

            @NotBlank(message = "Vui lòng nhập số điện thoại người liên hệ")
            @Pattern(
                    regexp = "^(0\\d{9}|\\+84\\d{9})$",
                    message = "Số điện thoại người liên hệ không hợp lệ"
            )
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
