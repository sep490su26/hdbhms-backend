package com.sep490.hdbhms.booking.infrastructure.web.dto.request;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Gender;
import com.sep490.hdbhms.shared.validator.Age;
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
    @NotNull(message = "Vui lòng chọn phòng")
    Long roomId;
    @NotBlank(message = "Vui lòng nhập họ và tên")
    String fullName;
    @NotNull(message = "Vui lòng nhập ngày sinh")
    @PastOrPresent(message = "Ngày sinh không được ở tương lai")
    @Age(age = 18, message = "Phải từ 18 tuổi trở lên")
    LocalDate dob;
    @NotBlank(message = "Vui lòng chọn giới tính")
    String gender;
    @Email(message = "Email không đúng định dạng")
    String email;
    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(
            regexp = "^0\\d{9}$",
            message = "Số điện thoại phải gồm 10 chữ số"
    )
    String phone;
    @NotBlank(message = "Vui lòng nhập địa chỉ")
    String permanentAddress;
    @NotBlank(message = "Vui lòng nhập số CCCD")
    @Pattern(
            regexp = "^(?:\\d{9}|\\d{10}|\\d{12})$",
            message = "Số CCCD phải có 12 chữ số"
    )
    String idNumber;
    @NotBlank(message = "Vui lòng nhập ngày cấp CCCD")
    String idIssueDate;
    @NotBlank(message = "Vui lòng nhập nơi cấp CCCD")
    String idIssuePlace;
    @NotNull(message = "Vui lòng nhập số tháng đặt cọc")
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

    @NotNull(message = "Vui lòng nhập số người ở")
    @Min(value = 1, message = "Số người ở phải lớn hơn 0")
    @Max(value = 3, message = "Số người ở không được vượt quá giá trị tối đa")
    Integer occupantCount;
    @Builder.Default
    List<CoOccupantRequest> coOccupants = new ArrayList<>();
    @NotNull(message = "Vui lòng nhập ngày dự kiến đến ở")
    @FutureOrPresent(message = "Ngày dự kiến đến ở không được ở quá khứ")
    LocalDate expectedMoveInDate;
    @NotNull(message = "Vui lòng nhập ngày dự kiến đến ký")
    @FutureOrPresent(message = "Ngày dự kiến đến ký không được ở quá khứ")
    LocalDate expectedLeaseSignDate;

    @AssertTrue(message = "Ngày dự kiến đến ở phải diễn ra sau ngày dự kiến đến ký")
    public boolean isExpectedMoveInAfterLeaseSign() {
        return expectedMoveInDate == null
                || expectedLeaseSignDate == null
                || !expectedMoveInDate.isBefore(expectedLeaseSignDate);
    }

    @AssertTrue(message = "Giới tính không hợp lệ.")
    public boolean isGenderValid() {
        return gender == null || Gender.fromLabel(gender) != Gender.UNKNOWN;
    }

    @AssertTrue(message = "Vui lòng nhập thông tin người ở cùng")
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
        @NotBlank(message = "Vui lòng nhập đầy đủ họ và tên")
        String fullName;
        @NotBlank(message = "Vui lòng nhập số điện thoại")
        @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số")
        String phone;
        @NotNull(message = "Vui lòng nhập thứ tự người ở cùng")
        @Min(value = 1, message = "Thứ tự người ở cùng không hợp lệ")
        @Max(value = 2, message = "Thứ tự người ở cùng không hợp lệ")
        Integer displayOrder;
    }
}
