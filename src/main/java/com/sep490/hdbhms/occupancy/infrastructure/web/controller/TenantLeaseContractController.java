package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.service.LeaseContractManagementService;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenant/contracts")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantLeaseContractController {
    LeaseContractManagementService leaseContractManagementService;

    @PostMapping("/{leaseContractId}/intention")
    @PreAuthorize("hasRole('TENANT')")
    public ApiResponse<LeaseContractManagementResponse> recordTenantIntention(
            @PathVariable Long leaseContractId,
            @Valid @RequestBody TenantIntentionRequest request
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.recordTenantIntentionForCurrentTenant(
                        leaseContractId,
                        request.intention(),
                        request.expectedMoveOutDate(),
                        request.note()
                ))
                .build();
    }

    public record TenantIntentionRequest(
            @NotNull(message = "Y dinh khach la bat buoc.")
            String intention,
            LocalDate expectedMoveOutDate,
            @Size(max = 1000, message = "Ghi chu khong duoc vuot qua 1000 ky tu.")
            String note
    ) {
    }
}
