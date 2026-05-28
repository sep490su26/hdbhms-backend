package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.shared.validator.Age;
import com.sep490.hdbhms.shared.validator.VietnamesePhone;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SendDepositFormRequest {
    @NotNull
    Long roomId;
    @NotBlank
    String fullName;
    @Age
    LocalDate dob;
    @Email
    String email;
    @NotBlank
    @VietnamesePhone
    String phone;
    @NotBlank
    String permanentAddress;
    @NotBlank
    @Pattern(
            regexp = "^(\\d{9}|\\d{12})$",
            message = "ID number must contain 9 or 12 digits"
    )
    String idNumber;
    @NotBlank
    String idIssueDate;
    @NotBlank
    String idIssuePlace;
    @NotNull
    @Future(message = "Move-in date must be in the future")
    LocalDate expectedMoveInDate;
    @NotNull
    @Future(message = "Lease sign date must be in the future")
    LocalDate expectedLeaseSignDate;
}
