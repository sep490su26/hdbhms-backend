package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.valueObjects.MeterType;
import com.sep490.hdbhms.occupancy.domain.valueObjects.ValidationStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MeterReadingImportRow {
    Long id;
    Long batchId;
    Integer rowNo;
    String roomCode;
    MeterType meterType;
    BigDecimal previousValue;
    BigDecimal currentValue;
    @Builder.Default
    ValidationStatus validationStatus = ValidationStatus.VALID;
    String validationMessage;
    LocalDateTime createdAt;
}
