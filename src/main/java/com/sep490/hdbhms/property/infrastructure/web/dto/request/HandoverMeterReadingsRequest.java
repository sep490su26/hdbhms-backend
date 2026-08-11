package com.sep490.hdbhms.property.infrastructure.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HandoverMeterReadingsRequest {

    @Valid
    @NotNull(message = "Chỉ số điện là bắt buộc")
    ReadingInput electricity;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ReadingInput {
        @NotNull(message = "Giá trị chỉ số điện là bắt buộc")
        @PositiveOrZero(message = "Chỉ số điện không được âm")
        BigDecimal currentValue;

        Long photoFileId;

        java.time.LocalDate readingDate;

        @NotNull(message = "Giá trị chỉ số nước là bắt buộc")
        @PositiveOrZero(message = "Chỉ số nước không được âm")
        BigDecimal waterValue;

        Long waterPhotoFileId;
    }
}
