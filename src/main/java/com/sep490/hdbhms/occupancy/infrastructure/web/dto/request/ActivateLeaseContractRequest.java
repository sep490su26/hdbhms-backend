package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActivateLeaseContractRequest {

    @Valid
    MeterInput electricity;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MeterInput {
        @NotNull(message = "Vui lòng nhập chỉ số điện")
        @PositiveOrZero(message = "Chỉ số điện không được âm")
        BigDecimal currentValue;

        Long photoFileId;
        LocalDate readingDate;
    }
}
