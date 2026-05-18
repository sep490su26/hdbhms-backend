package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingSource;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MeterReading {
    Long id;
    Long batchId;
    Long meterId;
    Long roomId;
    String readingPeriod;
    @Builder.Default
    Integer revisionNo = 1;
    BigDecimal previousValue;
    BigDecimal currentValue;
    BigDecimal usageAmount;
    LocalDate readingDate;
    Long photoFileId;
    ReadingSource source;
    @Builder.Default
    ReadingStatus status = ReadingStatus.CONFIRMED;
    String voidReason;
    Long createdById;
    LocalDateTime createdAt;
    String activeReadingKey;
}
