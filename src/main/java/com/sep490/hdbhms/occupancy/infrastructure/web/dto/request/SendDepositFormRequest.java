package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sep490.hdbhms.shared.validator.ValidPaymentCycle;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    @NotNull
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
    @NotBlank
    String permanentAddress;
    @NotBlank
    @Pattern(
            regexp = "^(?:\\d{9}|\\d{10}|\\d{12})$",
            message = "Số CCCD phải gồm 9, 10 hoặc 12 chữ số"
    )
    String idNumber;
    @NotBlank
    String idIssueDate;
    @NotBlank
    String idIssuePlace;
    @NotNull
    Integer depositMonths;
    @NotNull
    @ValidPaymentCycle
    @JsonProperty("payment_cycle_months")
    Integer paymentCycleMonths;
    @NotNull(message = "DEPOSIT_001")
    @Min(value = 1, message = "DEPOSIT_001")
    @Max(value = 3, message = "DEPOSIT_001")
    @JsonProperty("occupant_count")
    Integer occupantCount;
    @Builder.Default
    @JsonProperty("co_occupants")
    List<CoOccupantRequest> coOccupants = new ArrayList<>();
    @NotNull
    @FutureOrPresent(message = "Ngày dự kiến vào ở không được là ngày trong quá khứ")
    LocalDate expectedMoveInDate;
    @NotNull
    @FutureOrPresent(message = "Ngày hẹn ký hợp đồng không được là ngày trong quá khứ")
    LocalDate expectedLeaseSignDate;

    @AssertTrue(message = "Ngày dự kiến vào ở chỉ được tối đa 14 ngày kể từ hôm nay")
    public boolean isExpectedMoveInDateWithinAllowedRange() {
        return expectedMoveInDate == null || !expectedMoveInDate.isAfter(LocalDate.now().plusDays(14));
    }

    @AssertTrue(message = "Ngày hẹn ký hợp đồng chỉ được tối đa 14 ngày kể từ hôm nay")
    public boolean isExpectedLeaseSignDateWithinAllowedRange() {
        return expectedLeaseSignDate == null || !expectedLeaseSignDate.isAfter(LocalDate.now().plusDays(14));
    }

    @AssertTrue(message = "DEPOSIT_001")
    public boolean isCoOccupantInformationValid() {
        if (occupantCount == null || occupantCount < 1 || occupantCount > 3) {
            return false;
        }
        List<CoOccupantRequest> submittedCoOccupants = coOccupants == null ? List.of() : coOccupants;
        if (submittedCoOccupants.size() < occupantCount - 1) {
            return false;
        }
        List<CoOccupantRequest> visibleCoOccupants = submittedCoOccupants.stream()
                .filter(Objects::nonNull)
                .filter(coOccupant -> coOccupant.displayOrder != null && coOccupant.displayOrder < occupantCount)
                .sorted(java.util.Comparator.comparing(CoOccupantRequest::getDisplayOrder))
                .toList();
        if (visibleCoOccupants.size() != occupantCount - 1) {
            return false;
        }

        String mainPhone = normalizePhone(phone);
        List<String> phones = new ArrayList<>();
        for (int index = 0; index < visibleCoOccupants.size(); index++) {
            CoOccupantRequest coOccupant = visibleCoOccupants.get(index);
            if (coOccupant == null
                    || coOccupant.fullName == null
                    || coOccupant.fullName.isBlank()
                    || coOccupant.phone == null
                    || coOccupant.phone.isBlank()
                    || coOccupant.displayOrder == null
                    || coOccupant.displayOrder != index + 1) {
                return false;
            }
            String normalizedPhone = normalizePhone(coOccupant.phone);
            if (!isVietnamesePhone(normalizedPhone)
                    || Objects.equals(normalizedPhone, mainPhone)
                    || phones.contains(normalizedPhone)) {
                return false;
            }
            phones.add(normalizedPhone);
        }
        return true;
    }

    private static String normalizePhone(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\s.\\-()]", "");
    }

    private static boolean isVietnamesePhone(String value) {
        return value.matches("0\\d{9}");
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CoOccupantRequest {
        @NotBlank(message = "DEPOSIT_001")
        @JsonProperty("full_name")
        String fullName;
        @NotBlank(message = "DEPOSIT_001")
        @Pattern(regexp = "^0\\d{9}$", message = "DEPOSIT_001")
        String phone;
        @NotNull(message = "DEPOSIT_001")
        @Min(value = 1, message = "DEPOSIT_001")
        @Max(value = 2, message = "DEPOSIT_001")
        @JsonProperty("display_order")
        Integer displayOrder;
    }
}
