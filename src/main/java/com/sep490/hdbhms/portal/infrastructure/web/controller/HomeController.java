package com.sep490.hdbhms.portal.infrastructure.web.controller;

import com.sep490.hdbhms.portal.application.port.in.query.GetHomeQuery;
import com.sep490.hdbhms.portal.application.port.in.usecase.GetHomeUseCase;
import com.sep490.hdbhms.portal.infrastructure.web.dto.response.HomeResponse;
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
@RequestMapping("/api/v1/home")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HomeController {
    GetHomeUseCase getHomeUseCase;

    @GetMapping
    @PreAuthorize("hasRole('TENANT')")
    public ApiResponse<HomeResponse> getHome(
            @RequestHeader(value = "X-Client-Type", defaultValue = "web") String clientType
    ) {
        if (!"mobile".equals(clientType)) {
            throw new AppException(ApiErrorCode.UNAUTHORIZED);
        }
        Long userId = AuthUtils.getCurrentAuthenticationId();
        return ApiResponse.<HomeResponse>builder()
                .data(
                        getHomeUseCase.execute(
                                new GetHomeQuery(userId)
                        )
                )
                .build();
    }
}
