package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import java.util.List;

public record DepositFilterOptionsResponse(List<FloorOption> floors) {
    public record FloorOption(Long id, String name) {
    }
}
