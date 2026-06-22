package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

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

    @NotNull(message = "Property ID is required")
    Long propertyId;

    @NotBlank(message = "Reading period is required")
    @Pattern(regexp = "^(0[1-9]|1[0-2])/\\d{4}$", message = "Reading period must be in MM/yyyy format")
    String readingPeriod;

    @NotNull(message = "Reading date is required")
    LocalDate readingDate;

    @NotEmpty(message = "Readings list cannot be empty")
    @Valid
    List<RoomReadingInput> readings;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RoomReadingInput {
        @NotNull(message = "Room ID is required")
        Long roomId;

        @NotNull(message = "Electricity value is required")
        @PositiveOrZero(message = "Electricity value cannot be negative")
        BigDecimal electricityValue;

        @NotNull(message = "Water value is required")
        @PositiveOrZero(message = "Water value cannot be negative")
        BigDecimal waterValue;

        Long electricityPhotoId;
        Long waterPhotoId;
    }
}
