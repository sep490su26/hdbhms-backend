package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.valueObjects.BatchStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UtilityDashboardResponse {

    boolean canCreateCurrentPeriod;
    LocalDate nextAvailableDate;
    CurrentPeriodInfo currentPeriod;

    @Getter
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CurrentPeriodInfo {
        Long id;
        String readingPeriod;
        BatchStatus status;
    }
}
