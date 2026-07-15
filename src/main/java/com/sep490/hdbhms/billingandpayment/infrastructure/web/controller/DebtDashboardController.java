package com.sep490.hdbhms.billingandpayment.infrastructure.web.controller;

import com.sep490.hdbhms.billingandpayment.application.service.DebtDashboardService;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.DebtSummaryResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/admin/debts")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class DebtDashboardController {
    DebtDashboardService debtDashboardService;

    @GetMapping("/summary")
    public ApiResponse<List<DebtSummaryResponse>> getDebtSummary(
            @RequestParam(required = false) Long propertyId
    ) {
        return ApiResponse.<List<DebtSummaryResponse>>builder()
                .data(debtDashboardService.getDebtSummary(propertyId))
                .build();
    }
}
