package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.service.GetMeterReadingsService;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.SubmitMeterReadingUseCase;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.MeterReadingWebMapper;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.BatchMeterReadingRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.SingleMeterReadingRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.MeterReadingListResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meter-readings")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeterReadingController {

    MeterReadingWebMapper meterReadingWebMapper;
    GetMeterReadingsService getMeterReadingsService;
    SubmitMeterReadingUseCase submitMeterReadingUseCase;

    /**
     * GET /api/v1/meter-readings?period=MM/yyyy&propertyId=1
     * <p>
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

    /**
     * POST /api/v1/meter-readings/submit
     * <p>
     * Submits electricity and water readings for a single room.
     */
    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<Void> submitSingleReading(
            @Valid @RequestBody SingleMeterReadingRequest request) {
        submitMeterReadingUseCase.submitSingleReading(meterReadingWebMapper.toCommand(request));
        return ApiResponse.<Void>builder()
                .message("Meter readings submitted successfully")
                .build();
    }

    /**
     * POST /api/v1/meter-readings/batches
     * <p>
     * Submits electricity and water readings in batch for multiple rooms in a property.
     */
    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<Void> submitBatchReadings(
            @Valid @RequestBody BatchMeterReadingRequest request) {
        submitMeterReadingUseCase.submitBatchReadings(meterReadingWebMapper.toCommand(request));
        return ApiResponse.<Void>builder()
                .message("Batch meter readings submitted successfully")
                .build();
    }
}
