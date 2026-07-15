package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VisitRequestCreateRequest {
    @NotBlank(message = "VISIT_006")
    String customerName;

    @NotBlank(message = "VISIT_006")
    @Pattern(regexp = "^(0|\\+84)(\\d{9,10})$", message = "VISIT_003")
    String phone;

    @NotNull(message = "VISIT_006")
    Long propertyId;

    Long roomId;

    @NotNull(message = "VISIT_006")
    @Future(message = "VISIT_007")
    LocalDateTime appointmentAt;

    String note;
}
