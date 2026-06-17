package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
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

    @PastOrPresent(message = "Ngày sinh không được lớn hơn ngày hiện tại")
    LocalDate dob;

    @Email
    String email;

    @NotBlank
    @Pattern(
            regexp = "^0\\d{9}$",
            message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0"
    )
    String phone;

    String permanentAddress;

    String idNumber;

    LocalDate idIssueDate;

    String idIssuePlace;

    @NotNull
    @FutureOrPresent(message = "Ngày dự kiến vào ở không được là ngày trong quá khứ")
    LocalDate expectedMoveInDate;

    @NotNull
    @FutureOrPresent(message = "Ngày hẹn ký hợp đồng không được là ngày trong quá khứ")
    LocalDate expectedLeaseSignDate;

    @NotNull
    @JsonProperty("payment_cycle_months")
    Integer paymentCycleMonths;

    @AssertTrue(message = "Ngày dự kiến vào ở chỉ được tối đa 14 ngày kể từ hôm nay")
    public boolean isExpectedMoveInDateWithinAllowedRange() {
        return expectedMoveInDate == null || !expectedMoveInDate.isAfter(LocalDate.now().plusDays(14));
    }

    @AssertTrue(message = "Ngày hẹn ký hợp đồng chỉ được tối đa 14 ngày kể từ hôm nay")
    public boolean isExpectedLeaseSignDateWithinAllowedRange() {
        return expectedLeaseSignDate == null || !expectedLeaseSignDate.isAfter(LocalDate.now().plusDays(14));
    }
}
