package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateLeaseContractActivationReadingRequest {

    @PositiveOrZero(message = "Chỉ số điện không được âm")
    BigDecimal currentValue;

    LocalDate readingDate;
}
