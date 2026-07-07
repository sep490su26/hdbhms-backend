package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.valueObjects.ReadingPurpose;
import com.sep490.hdbhms.occupancy.domain.valueObjects.ReadingSource;
import com.sep490.hdbhms.occupancy.domain.valueObjects.ReadingStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
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
    @Setter
    BigDecimal currentValue;
    BigDecimal usageAmount;
    LocalDate readingDate;
    @Setter
    Long photoFileId;
    @Builder.Default
    ReadingPurpose purpose = ReadingPurpose.MONTHLY;
    @Builder.Default
    ReadingSource source = ReadingSource.MANUAL;
    @Builder.Default
    ReadingStatus status = ReadingStatus.CONFIRMED;
    String voidReason;
    Long createdById;
    LocalDateTime createdAt;
    String activeReadingKey;
}
