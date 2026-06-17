package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.BatchSource;
import com.sep490.hdbhms.occupancy.domain.value_objects.BatchStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MeterReadingBatch {
    Long id;
    Long propertyId;
    String readingPeriod;
    @Builder.Default
    Integer totalRooms = 0;
    @Builder.Default
    Integer completedRooms = 0;
    @Builder.Default
    Integer anomalyCount = 0;
    @Setter
    @Builder.Default
    BatchStatus status = BatchStatus.DRAFT;
    Long importedFileId;
    Long createdById;
    @Setter
    Long confirmedById;
    @Setter
    LocalDateTime confirmedAt;
    LocalDateTime createdAt;
}
