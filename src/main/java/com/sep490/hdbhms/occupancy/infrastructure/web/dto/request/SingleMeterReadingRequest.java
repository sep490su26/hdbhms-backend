package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

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

    @NotNull(message = "Room ID is required")
    Long roomId;

    @NotBlank(message = "Reading period is required")
    @Pattern(regexp = "^(0[1-9]|1[0-2])/\\d{4}$", message = "Reading period must be in MM/yyyy format")
    String readingPeriod;

    @NotNull(message = "Reading date is required")
    LocalDate readingDate;

    @NotNull(message = "Electricity value is required")
    @PositiveOrZero(message = "Electricity value cannot be negative")
    BigDecimal electricityValue;

    @NotNull(message = "Water value is required")
    @PositiveOrZero(message = "Water value cannot be negative")
    BigDecimal waterValue;

    Long electricityPhotoId;
    Long waterPhotoId;
}
