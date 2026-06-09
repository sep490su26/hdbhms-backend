package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sep490.hdbhms.shared.validator.Age;
import com.sep490.hdbhms.shared.validator.ValidPaymentCycle;
import com.sep490.hdbhms.shared.validator.VietnamesePhone;
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
    @Future(message = "Move-in date must be in the future")
    LocalDate expectedMoveInDate;
    @NotNull
    @Future(message = "Lease sign date must be in the future")
    LocalDate expectedLeaseSignDate;

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
        String cleaned = value.replaceAll("[\\s.\\-()]", "");
        if (cleaned.startsWith("+84")) {
            return "0" + cleaned.substring(3);
        }
        return cleaned;
    }

    private static boolean isVietnamesePhone(String value) {
        return value.matches("0[35789]\\d{8}");
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
        @VietnamesePhone(message = "DEPOSIT_001")
        String phone;
        @NotNull(message = "DEPOSIT_001")
        @Min(value = 1, message = "DEPOSIT_001")
        @Max(value = 2, message = "DEPOSIT_001")
        @JsonProperty("display_order")
        Integer displayOrder;
    }
}
