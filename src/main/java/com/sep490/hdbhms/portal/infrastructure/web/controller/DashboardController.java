package com.sep490.hdbhms.portal.infrastructure.web.controller;

import com.sep490.hdbhms.portal.application.port.in.query.GetDashboardQuery;
import com.sep490.hdbhms.portal.application.port.in.usecase.GetDashboardUseCase;
import com.sep490.hdbhms.portal.infrastructure.web.dto.response.DashboardResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardController {
    GetDashboardUseCase getDashboardUseCase;

    @GetMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('MANAGER') or hasRole('ACCOUNTANT')")
    public ApiResponse<DashboardResponse> getDashboard(
            @RequestHeader(value = "X-Client-Type", defaultValue = "web") String clientType
    ) {
        if (!"web".equals(clientType)) {
            throw new AppException(ApiErrorCode.UNAUTHORIZED);
        }
        Long userId = AuthUtils.getCurrentAuthenticationId();
        return ApiResponse.<DashboardResponse>builder()
                .data(
                        getDashboardUseCase.execute(
                                new GetDashboardQuery(userId)
                        )
                )
                .build();
    }
}
