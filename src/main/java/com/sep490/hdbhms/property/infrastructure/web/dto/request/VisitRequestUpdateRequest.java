package com.sep490.hdbhms.property.infrastructure.web.dto.request;

import com.sep490.hdbhms.property.domain.value_objects.VisitRequestStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VisitRequestUpdateRequest {
    @NotBlank(message = "Vui lòng nhập tên người xem")
    String customerName;

    @NotBlank(message = "Vui lòng nhập số điện thoại người xem")
    @Pattern(regexp = "^(0|\\+84)(\\d{9,10})$", message = "Số điện thoại phải là số Việt Nam gồm 10 chữ số và bắt đầu bằng 0.")
    String phone;

    @NotNull(message = "VISIT_006")
    Long propertyId;

    Long roomId;

    @NotNull(message = "Vui lòng nhập ngày đến xem")
    @Future(message = "Thời gian xem phải ở tương lai")
    LocalDateTime appointmentAt;

    String note;
    VisitRequestStatus status;
}
