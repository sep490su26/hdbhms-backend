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
}
