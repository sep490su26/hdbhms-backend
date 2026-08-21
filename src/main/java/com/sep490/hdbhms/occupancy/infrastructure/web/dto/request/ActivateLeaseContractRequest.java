package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @Valid
    InitialRentPayment initialRentPayment;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class InitialRentPayment {
        @NotNull(message = "Vui lòng nhập số tiền phòng kỳ đầu đã thu")
        @Positive(message = "Số tiền phòng kỳ đầu phải lớn hơn 0")
        Long amount;

        String payerName;
        String note;
    }

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
