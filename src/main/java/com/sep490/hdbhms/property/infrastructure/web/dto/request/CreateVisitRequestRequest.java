package com.sep490.hdbhms.property.infrastructure.web.dto.request;

import com.sep490.hdbhms.shared.validator.FullName;
import com.sep490.hdbhms.shared.validator.VietnamesePhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotBlank(message = "Vui lòng nhập tên người xem")
    @FullName(message = "Vui lòng nhập tên người xem")
    String visitorName;
    @NotBlank(message = "Vui lòng nhập số điện thoại người xem")
    @VietnamesePhone(message = "Số điện thoại phải là số Việt Nam gồm 10 chữ số và bắt đầu bằng 0.")
    String visitorPhone;
    @Email(message = "Địa chỉ email sai định dạng")
    String visitorEmail;
    @NotNull(message = "Vui lòng nhập ngày đến xem")
    @Future(message = "Thời gian xem phải ở tương lai")
    LocalDateTime preferredStart;
    String notes;
}
