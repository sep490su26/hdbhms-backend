package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.sep490.hdbhms.shared.validator.Age;
import com.sep490.hdbhms.shared.validator.ValidPaymentCycle;
import com.sep490.hdbhms.shared.validator.VietnamesePhone;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class BatchDepositCheckoutRequest {
    @NotEmpty
    @Size(min = 1)
    @Valid
    List<RoomRequest> rooms;

    @NotBlank
    @Pattern(regexp = "^[\\p{L}\\s]+$")
    String fullName;

    @NotNull
    @Age
    LocalDate dob;

    @NotBlank
    @VietnamesePhone
    String phone;

    @Email
    String email;

    @NotBlank
    @Pattern(
            regexp = "^(?:\\d{9}|\\d{10}|\\d{12})$",
            message = "Số CCCD phải gồm 9, 10 hoặc 12 chữ số."
    )
    String idNumber;

    @NotNull
    @PastOrPresent
    LocalDate idIssueDate;

    @NotBlank
    String idIssuePlace;

    @NotBlank
    String permanentAddress;

    @NotNull
    @Future
    LocalDate expectedMoveInDate;

    @NotNull
    @Future
    LocalDate expectedLeaseSignDate;

    @NotNull
    @Positive
    Integer depositMonths;

    @NotNull
    @ValidPaymentCycle
    Integer paymentCycleMonths;

    @AssertTrue(message = "Thông tin người ở cùng không hợp lệ.")
    public boolean isCoOccupantInformationValid() {
        if (rooms == null || phone == null) {
            return true;
        }
        String mainPhone = normalizePhone(phone);
        for (RoomRequest room : rooms) {
            if (room == null || room.occupantCount == null) {
                continue;
            }
            List<CoOccupantRequest> submitted = room.coOccupants == null ? List.of() : room.coOccupants;
            if (submitted.size() != room.occupantCount - 1) {
                return false;
            }
            Set<String> phones = new HashSet<>();
            for (int index = 0; index < submitted.size(); index++) {
                CoOccupantRequest occupant = submitted.get(index);
                if (occupant == null
                        || occupant.displayOrder == null
                        || occupant.displayOrder != index + 1
                        || occupant.fullName == null
                        || occupant.fullName.isBlank()) {
                    return false;
                }
                String occupantPhone = normalizePhone(occupant.phone);
                if (!isVietnamesePhone(occupantPhone)
                        || Objects.equals(occupantPhone, mainPhone)
                        || !phones.add(occupantPhone)) {
                    return false;
                }
            }
        }
        return true;
    }

    @AssertTrue(message = "Ngày cấp CCCD không được trước ngày sinh.")
    public boolean isIdentityIssueDateValid() {
        return dob == null || idIssueDate == null || !idIssueDate.isBefore(dob);
    }

    @AssertTrue(message = "Danh sách phòng có roomId trùng lặp.")
    public boolean isRoomListUnique() {
        if (rooms == null) {
            return true;
        }
        Set<Long> roomIds = new HashSet<>();
        return rooms.stream()
                .filter(Objects::nonNull)
                .map(RoomRequest::getRoomId)
                .allMatch(roomId -> roomId == null || roomIds.add(roomId));
    }

    private static String normalizePhone(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replaceAll("[\\s.\\-()]", "");
        return cleaned.startsWith("+84") ? "0" + cleaned.substring(3) : cleaned;
    }

    private static boolean isVietnamesePhone(String value) {
        return value.matches("0[35789]\\d{8}");
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class RoomRequest {
        @NotNull
        Long roomId;

        @NotNull
        @Positive
        Integer occupantCount;

        @Builder.Default
        @Valid
        List<CoOccupantRequest> coOccupants = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class CoOccupantRequest {
        @NotBlank
        String fullName;

        @NotBlank
        @VietnamesePhone
        String phone;

        @NotNull
        @Positive
        Integer displayOrder;
    }
}
