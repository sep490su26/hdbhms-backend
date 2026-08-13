package com.sep490.hdbhms.property.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SingleMeterReadingRequest {

    @NotNull(message = "Vui lòng chọn phòng")
    Long roomId;

    @NotBlank(message = "Vui lòng chọn kỳ ghi chỉ số")
    @Pattern(
            regexp = "^(?:(0?[1-9]|1[0-2])[-/]\\d{4}|\\d{4}-(0?[1-9]|1[0-2]))$",
            message = "Kỳ ghi chỉ số phải có định dạng MMyyyy"
    )
    String readingPeriod;

    @NotNull(message = "Vui lòng chọn ngày ghi")
    LocalDate readingDate;

    @NotNull(message = "Vui lòng nhập chỉ số điện")
    @PositiveOrZero(message = "Chỉ số điện không được nhỏ hơn 0")
    BigDecimal electricityValue;

    Long electricityPhotoId;
}
