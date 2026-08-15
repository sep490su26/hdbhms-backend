package com.sep490.hdbhms.booking.infrastructure.web.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Gender;
import com.sep490.hdbhms.shared.validator.ValidPaymentCycle;
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
    @NotEmpty(message = "Danh sách phòng không được trống")
    @Size(min = 1, message = "Danh sách phòng không được trống")
    @Valid
    List<RoomRequest> rooms;

    @NotBlank(message = "Vui lòng nhập họ và tên")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Họ và tên không hợp lệ")
    String fullName;

    @NotNull(message = "Vui lòng nhập ngày sinh")
    @PastOrPresent(message = "Ngày sinh không được ở tương lai")
    LocalDate dob;

    @NotBlank(message = "Vui lòng chọn giới tính")
    String gender;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(
            regexp = "^0\\d{9}$",
            message = "Số điện thoại phải gồm 10 chữ số"
    )
    String phone;

    @Email(message = "Email không đúng định dạng")
    String email;

    @NotBlank(message = "Vui lòng nhập số CCCD")
    @Pattern(regexp = "^\\d{12}$", message = "Số CCCD phải có 12 chữ số")
    String idNumber;

    @NotNull(message = "Vui lòng nhập ngày cấp CCCD")
    @PastOrPresent(message = "Ngày cấp CCCD không được ở tương lai")
    LocalDate idIssueDate;

    @NotBlank(message = "Vui lòng nhập nơi cấp CCCD")
    String idIssuePlace;

    @NotBlank(message = "Vui lòng nhập địa chỉ")
    String permanentAddress;

    @NotNull(message = "Vui lòng nhập ngày dự kiến đến ở")
    @FutureOrPresent(message = "Ngày dự kiến đến ở không được ở quá khứ")
    LocalDate expectedMoveInDate;

    @NotNull(message = "Vui lòng nhập ngày dự kiến đến ký")
    @FutureOrPresent(message = "Ngày dự kiến đến ký không được ở quá khứ")
    LocalDate expectedLeaseSignDate;

    @NotNull(message = "Vui lòng nhập số tháng đặt cọc")
    @Positive(message = "Số tháng đặt cọc phải lớn hơn 0")
    Integer depositMonths;

    @NotNull(message = "Vui lòng nhập thời hạn hợp đồng")
    @Min(value = 6, message = "Thời hạn hợp đồng tối thiểu là 6 tháng.")
    Integer contractTermMonths;

    @NotNull(message = "Vui lòng nhập chu kỳ thanh toán")
    @ValidPaymentCycle(message = "Chu kỳ thanh toán chỉ nhận 1 hoặc 3 tháng")
    Integer paymentCycleMonths;

    @AssertTrue(message = "Thời hạn hợp đồng phải là bội số của chu kỳ thanh toán")
    public boolean isContractTermMonthsValid() {
        return contractTermMonths == null
                || paymentCycleMonths == null
                || paymentCycleMonths <= 0
                || contractTermMonths % paymentCycleMonths == 0;
    }

    @AssertTrue(message = "Ngày dự kiến vào ở chỉ được tối đa 14 ngày kể từ hôm nay")
    public boolean isExpectedMoveInDateWithinAllowedRange() {
        return expectedMoveInDate == null || !expectedMoveInDate.isAfter(LocalDate.now().plusDays(14));
    }

    @AssertTrue(message = "Ngày hẹn ký hợp đồng chỉ được tối đa 14 ngày kể từ hôm nay")
    public boolean isExpectedLeaseSignDateWithinAllowedRange() {
        return expectedLeaseSignDate == null || !expectedLeaseSignDate.isAfter(LocalDate.now().plusDays(14));
    }

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

    @AssertTrue(message = "Giới tính không hợp lệ.")
    public boolean isGenderValid() {
        return gender == null || Gender.fromLabel(gender) != Gender.UNKNOWN;
    }

    @AssertTrue(message = "Danh sách phòng không được chứa phòng trùng")
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
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class RoomRequest {
        @NotNull(message = "Vui lòng chọn phòng")
        Long roomId;

        @NotNull(message = "Vui lòng nhập số người ở")
        @Positive(message = "Số người ở phải lớn hơn 0")
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
        @NotBlank(message = "Vui lòng nhập đầy đủ họ và tên")
        String fullName;

        @NotBlank(message = "Vui lòng nhập số điện thoại")
        @Pattern(
                regexp = "^0\\d{9}$",
                message = "Số điện thoại phải gồm 10 chữ số"
        )
        String phone;

        @NotNull(message = "Vui lòng nhập thứ tự người ở cùng")
        @Positive(message = "Thứ tự người ở cùng không hợp lệ")
        Integer displayOrder;
    }
}
