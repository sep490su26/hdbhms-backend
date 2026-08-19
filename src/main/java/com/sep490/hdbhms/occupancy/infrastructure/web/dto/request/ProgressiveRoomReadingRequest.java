package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProgressiveRoomReadingRequest {

    @NotNull(message = "Giá trị chỉ số điện là bắt buộc")
    @PositiveOrZero(message = "Chỉ số điện không hợp lệ")
    BigDecimal electricityValue;

    Long electricityPhotoId;

    @PositiveOrZero
    Integer rolloverCount;
}
