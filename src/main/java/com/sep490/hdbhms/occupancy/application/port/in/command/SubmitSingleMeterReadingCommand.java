package com.sep490.hdbhms.occupancy.application.port.in.command;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubmitSingleMeterReadingCommand(
        Long roomId,
        String readingPeriod,
        LocalDate readingDate,
        BigDecimal electricityValue,
        BigDecimal waterValue,
        Long electricityPhotoId,
        Long waterPhotoId
) {
}
