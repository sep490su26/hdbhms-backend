package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.service.TenantHandoverService;
import com.sep490.hdbhms.occupancy.domain.valueObjects.HandoverType;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.ContractHandoverDetailsResponse;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenant/contracts/{contractId}")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantHandoverController {

    TenantHandoverService tenantHandoverService;

    @GetMapping("/handover-items")
    @PreAuthorize("hasRole('TENANT')")
    public ApiResponse<ContractHandoverDetailsResponse> getHandoverItems(
            @PathVariable Long contractId,
            @RequestParam(required = false, defaultValue = "MOVE_IN") HandoverType type
    ) {
        return ApiResponse.<ContractHandoverDetailsResponse>builder()
                .data(tenantHandoverService.getHandoverItems(contractId, type))
                .build();
    }
}
