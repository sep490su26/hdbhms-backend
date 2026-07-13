package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.service.GetMeterReadingsService;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.SubmitMeterReadingUseCase;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.MeterReadingWebMapper;
import com.sep490.hdbhms.occupancy.application.service.GetBatchMeterReadingsService;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.BatchMeterReadingRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.SingleMeterReadingRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.BatchMeterReadingStatusResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.MeterReadingBatchHistoryResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.MeterReadingListResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.UtilityDashboardResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.ProgressiveRoomReadingRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meter-readings")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeterReadingController {

    MeterReadingWebMapper meterReadingWebMapper;
    GetMeterReadingsService getMeterReadingsService;
    GetBatchMeterReadingsService getBatchMeterReadingsService;
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
     * GET /api/v1/meter-readings/batch-status?period=MM/yyyy&propertyId=1
     * <p>
     * Returns rooms that have lease contracts overlapping the period for the batch input UI.
     */
    @GetMapping("/batch-status")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<BatchMeterReadingStatusResponse> getBatchStatus(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Long propertyId) {
        return ApiResponse.<BatchMeterReadingStatusResponse>builder()
                .data(getBatchMeterReadingsService.getBatchStatus(period, propertyId))
                .build();
    }

    /**
     * GET /api/v1/meter-readings/history?propertyId=1
     * <p>
     * Returns history of batch meter readings.
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<MeterReadingBatchHistoryResponse> getBatchHistory(
            @RequestParam(required = false) Long propertyId) {
        return ApiResponse.<MeterReadingBatchHistoryResponse>builder()
                .data(getBatchMeterReadingsService.getBatchHistory(propertyId))
                .build();
    }

    /**
     * GET /api/v1/meter-readings/dashboard?propertyId=1
     * <p>
     * Returns dashboard information for meter readings.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<UtilityDashboardResponse> getDashboard(
            @RequestParam(required = false) Long propertyId) {
        return ApiResponse.<UtilityDashboardResponse>builder()
                .data(getBatchMeterReadingsService.getDashboard(propertyId))
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

    @PostMapping("/batches/start")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<Long> startBatch(
            @RequestParam String period,
            @RequestParam Long propertyId) {
        Long batchId = submitMeterReadingUseCase.startBatch(period, propertyId);
        return ApiResponse.<Long>builder()
                .data(batchId)
                .message("Batch started successfully")
                .build();
    }

    @PutMapping("/batches/{batchId}/rooms/{roomId}")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<Void> saveProgressiveRoomReading(
            @PathVariable Long batchId,
            @PathVariable Long roomId,
            @Valid @RequestBody ProgressiveRoomReadingRequest request) {
        submitMeterReadingUseCase.saveProgressiveRoomReading(
                batchId, roomId, 
                request.getElectricityValue(), request.getWaterValue(), 
                request.getElectricityPhotoId(), request.getWaterPhotoId()
        );
        return ApiResponse.<Void>builder()
                .message("Room reading saved successfully")
                .build();
    }

    @PostMapping("/batches/{batchId}/confirm")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<Void> confirmBatch(
            @PathVariable Long batchId) {
        submitMeterReadingUseCase.confirmBatch(batchId);
        return ApiResponse.<Void>builder()
                .message("Batch confirmed successfully")
                .build();
    }
}
