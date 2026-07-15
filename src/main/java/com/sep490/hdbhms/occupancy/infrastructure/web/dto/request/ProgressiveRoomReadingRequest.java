package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProgressiveRoomReadingRequest {

    @NotNull(message = "Electricity value is required")
    BigDecimal electricityValue;

    @NotNull(message = "Water value is required")
    BigDecimal waterValue;

    Long electricityPhotoId;
    Long waterPhotoId;
}
