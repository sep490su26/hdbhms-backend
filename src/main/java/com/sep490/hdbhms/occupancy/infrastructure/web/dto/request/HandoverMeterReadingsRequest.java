package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

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
    @NotNull
    ReadingInput electricity;

    @Valid
    @NotNull
    ReadingInput water;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ReadingInput {
        @NotNull(message = "Current value is required")
        @PositiveOrZero(message = "Value cannot be negative")
        BigDecimal currentValue;
        
        Long photoFileId;

        java.time.LocalDate readingDate;
    }
}
