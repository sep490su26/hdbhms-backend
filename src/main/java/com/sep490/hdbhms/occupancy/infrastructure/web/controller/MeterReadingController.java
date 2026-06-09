package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.service.GetMeterReadingsService;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.MeterReadingListResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meter-readings")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeterReadingController {

    GetMeterReadingsService getMeterReadingsService;

    /**
     * GET /api/v1/meter-readings?period=MM/yyyy&propertyId=1
     *
     * Returns readings grouped by room.
     * - period: defaults to current month if omitted
     * - propertyId: optional; returns all properties if omitted
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<MeterReadingListResponse> getReadings(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Long propertyId) {
        return ApiResponse.<MeterReadingListResponse>builder()
                .data(getMeterReadingsService.getReadings(period, propertyId))
                .build();
    }
}
