package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.service.LeaseContractQueryService;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractQueryDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractQueryItemResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomRentalHistoryResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenants/{tenantId}")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantLeaseContractController {
    LeaseContractQueryService leaseContractQueryService;

    @GetMapping("/contracts")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','TENANT')")
    public ApiResponse<List<LeaseContractQueryItemResponse>> getContracts(
            @PathVariable Long tenantId,
            @RequestParam(required = false) LeaseStatus status,
            @RequestParam(name = "room_id", required = false) Long roomId,
            @RequestParam(name = "property_id", required = false) Long propertyId,
            @RequestParam(name = "tenant_profile_id", required = false) Long tenantProfileId,
            @RequestParam(name = "date_from", required = false) LocalDate dateFrom,
            @RequestParam(name = "date_to", required = false) LocalDate dateTo,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.<List<LeaseContractQueryItemResponse>>builder()
                .data(leaseContractQueryService.findContracts(
                        tenantId,
                        status,
                        roomId,
                        propertyId,
                        tenantProfileId,
                        dateFrom,
                        dateTo,
                        keyword
                ))
                .build();
    }

    @GetMapping("/contracts/{contractId}")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','TENANT')")
    public ApiResponse<LeaseContractQueryDetailsResponse> getContractDetails(
            @PathVariable Long tenantId,
            @PathVariable Long contractId
    ) {
        return ApiResponse.<LeaseContractQueryDetailsResponse>builder()
                .data(leaseContractQueryService.getContractDetails(tenantId, contractId))
                .build();
    }

    @GetMapping("/rooms/{roomId}/rental-history")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','TENANT')")
    public ApiResponse<RoomRentalHistoryResponse> getRoomRentalHistory(
            @PathVariable Long tenantId,
            @PathVariable Long roomId
    ) {
        return ApiResponse.<RoomRentalHistoryResponse>builder()
                .data(leaseContractQueryService.getRoomRentalHistory(tenantId, roomId))
                .build();
    }
}
