package com.sep490.hdbhms.property.infrastructure.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BatchMeterReadingRequest {

    @NotNull(message = "Vui lòng chọn cơ sở")
    Long propertyId;

    @NotBlank(message = "Vui lòng chọn kỳ ghi chỉ số")
    @Pattern(
            regexp = "^(?:(0?[1-9]|1[0-2])[-/]\\d{4}|\\d{4}-(0?[1-9]|1[0-2]))$",
            message = "Kỳ ghi chỉ số phải có định dạng MM-yyyy"
    )
    String readingPeriod;

    @NotNull(message = "Vui lòng chọn ngày ghi chỉ số")
    LocalDate readingDate;

    @NotEmpty(message = "Danh sách chỉ số không được để trống")
    @Valid
    List<RoomReadingInput> readings;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RoomReadingInput {
        @NotNull(message = "Vui lòng chọn phòng")
        Long roomId;

        @NotNull(message = "Vui lòng nhập chỉ số điện")
        @PositiveOrZero(message = "Chỉ số điện không được âm")
        BigDecimal electricityValue;

        Long electricityPhotoId;

    }
}
