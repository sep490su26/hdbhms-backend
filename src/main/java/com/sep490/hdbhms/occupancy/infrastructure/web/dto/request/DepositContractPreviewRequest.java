package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.shared.validator.VietnamesePhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositContractPreviewRequest {
    @NotNull
    Long roomId;

    @NotBlank
    String fullName;

    LocalDate dob;

    @Email
    String email;

    @NotBlank
    @VietnamesePhone
    String phone;

    String permanentAddress;

    String idNumber;

    LocalDate idIssueDate;

    String idIssuePlace;

    @NotNull
    LocalDate expectedMoveInDate;

    @NotNull
    LocalDate expectedLeaseSignDate;
}
