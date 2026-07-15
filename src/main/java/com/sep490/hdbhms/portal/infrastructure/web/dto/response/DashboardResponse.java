package com.sep490.hdbhms.portal.infrastructure.web.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardResponse {
    Long totalOccupiedRoomCount;
    Long totalRoomCount;
    Long totalVacantRoomCount;
    List<FloorEfficiencyResponse> floorEfficiencies;
    Long currentMonthRevenue;
    Long previousMonthRevenue;
    Double revenueGrowthPercent;
    List<RevenuePointResponse> revenueSeries;
    Long totalDebtAmount;
    Long debtWarningRoomCount;
    UtilityUsageResponse utilityUsage;
    ExpiringContractSummaryResponse expiringContractSummary;
    List<RecentActivityResponse> recentActivities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RevenuePointResponse {
        String period;
        String label;
        Long amount;
        Integer percentOfPeak;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UtilityUsageResponse {
        String period;
        Double electricityUsage;
        Double waterUsage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ExpiringContractSummaryResponse {
        Long count;
        List<ExpiringTenantResponse> tenants;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ExpiringTenantResponse {
        String fullName;
        String initials;
        String roomName;
        String endDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RecentActivityResponse {
        String type;
        String title;
        String time;
        String tone;
        String occurredAt;
    }
}
