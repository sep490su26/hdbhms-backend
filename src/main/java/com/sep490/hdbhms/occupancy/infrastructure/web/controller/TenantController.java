package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetMyTenantProfileUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UpdateMyTenantProfileUseCase;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.UpdateTenantProfileRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.TenantProfileResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenants")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantController {
    GetMyTenantProfileUseCase getMyTenantProfileUseCase;
    UpdateMyTenantProfileUseCase updateMyTenantProfileUseCase;

    @GetMapping("/profiles/me")
    public ApiResponse<TenantProfileResponse> getMyTenantProfile() {
        return ApiResponse.<TenantProfileResponse>builder()
                .data(getMyTenantProfileUseCase.execute())
                .build();
    }

    @PutMapping("/profiles/me")
    public ApiResponse<TenantProfileResponse> updateMyTenantProfile(
            @Valid @RequestBody UpdateTenantProfileRequest request
    ) {
        return ApiResponse.<TenantProfileResponse>builder()
                .data(updateMyTenantProfileUseCase.execute(request))
                .build();
    }
}
