package com.sep490.hdbhms.portal.infrastructure.web.controller;

import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.application.service.GetFacilitiesDashboardService;
import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.FacilitiesDashboardResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard/facilities")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FacilitiesDashboardController {
    GetFacilitiesDashboardService facilitiesDashboardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<FacilitiesDashboardResponse> getFacilities(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PropertyStatus status,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ApiResponse.<FacilitiesDashboardResponse>builder()
                .data(facilitiesDashboardService.getDashboard(
                        principal.getId(),
                        principal.getRole(),
                        keyword,
                        status,
                        pageable
                ))
                .build();
    }
}
