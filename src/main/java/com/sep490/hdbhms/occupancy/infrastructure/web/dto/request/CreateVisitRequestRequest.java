package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.shared.validator.FullName;
import com.sep490.hdbhms.shared.validator.VietnamesePhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateVisitRequestRequest {
    @NonNull
    Long propertyId;
    Long roomId;
    @NotBlank
    @FullName
    String visitorName;
    @NotBlank
    @VietnamesePhone
    String visitorPhone;
    @Email
    String visitorEmail;
    @Future(message = "Preferred start date must be in the future")
    LocalDateTime preferredStart;
    String notes;
}
