package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.command.RecordTenantIntentionCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.RecordTenantIntentionUseCase;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.shared.types.dto.response.ApiResponse;
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
    RecordTenantIntentionUseCase recordTenantIntentionUseCase;

    @PostMapping("/{leaseContractId}/intention")
    @PreAuthorize("hasRole('TENANT')")
    public ApiResponse<LeaseContractManagementResponse> recordTenantIntention(
            @PathVariable Long leaseContractId,
            @Valid @RequestBody TenantIntentionRequest request
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .message("Ghi nhận ý định thành công")
                .data(recordTenantIntentionUseCase.executeForCurrentTenant(new RecordTenantIntentionCommand(
                        leaseContractId,
                        request.intention(),
                        request.expectedMoveOutDate(),
                        request.note()
                )))
                .build();
    }

    public record TenantIntentionRequest(
            @NotNull(message = "Vui lòng chọn ý định")
            String intention,
            LocalDate expectedMoveOutDate,
            @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự.")
            String note
    ) {
    }
}
