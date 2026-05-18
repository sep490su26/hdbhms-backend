package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.BatchSource;
import com.sep490.hdbhms.occupancy.domain.value_objects.BatchStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MeterReadingBatch {
    Long id;
    Long propertyId;
    String readingPeriod;
    BatchSource source;
    @Builder.Default
    BatchStatus status = BatchStatus.DRAFT;
    Long importedFileId;
    Long createdById;
    Long confirmedById;
    LocalDateTime confirmedAt;
    LocalDateTime createdAt;
}
