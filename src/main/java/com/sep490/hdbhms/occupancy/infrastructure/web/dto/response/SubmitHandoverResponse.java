package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.valueObjects.HandoverStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.HandoverType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitHandoverResponse {

    Long handoverRecordId;
    HandoverType handoverType;
    HandoverStatus status;
    LocalDateTime handoverDate;

    Long electricityReadingId;
    Long waterReadingId;

    List<AssetResult> assets;

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AssetResult {
        Long id;
        String assetName;
        boolean created; // true = newly created, false = updated
    }
}
