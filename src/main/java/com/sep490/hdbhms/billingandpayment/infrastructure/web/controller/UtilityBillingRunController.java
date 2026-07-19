package com.sep490.hdbhms.billingandpayment.infrastructure.web.controller;

import com.sep490.hdbhms.billingandpayment.application.service.UtilityBillingRunService;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.UtilityBillingItemAdjustmentRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.UtilityBillingRunResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/admin/utility-billing-runs")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class UtilityBillingRunController {
    UtilityBillingRunService utilityBillingRunService;

    @GetMapping
    public ApiResponse<List<UtilityBillingRunResponse>> listRuns(
            @RequestParam(required = false) String billingPeriod,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.<List<UtilityBillingRunResponse>>builder()
                .data(utilityBillingRunService.listRuns(billingPeriod, propertyId, status))
                .build();
    }

    @PostMapping("/properties/{propertyId}")
    public ApiResponse<UtilityBillingRunResponse> createPreview(
            @PathVariable Long propertyId,
            @RequestParam String billingPeriod,
            @RequestParam(required = false) String invoiceReason
    ) {
        return ApiResponse.<UtilityBillingRunResponse>builder()
                .data(utilityBillingRunService.createPreview(
                        propertyId,
                        billingPeriod,
                        invoiceReason,
                        AuthUtils.getCurrentAuthenticationId()
                ))
                .build();
    }

    @GetMapping("/{runId}")
    public ApiResponse<UtilityBillingRunResponse> getRun(@PathVariable Long runId) {
        return ApiResponse.<UtilityBillingRunResponse>builder()
                .data(utilityBillingRunService.getRun(runId))
                .build();
    }

    @PatchMapping("/{runId}/items/{itemId}/adjustment")
    public ApiResponse<UtilityBillingRunResponse> updateAdjustment(
            @PathVariable Long runId,
            @PathVariable Long itemId,
            @RequestBody(required = false) UtilityBillingItemAdjustmentRequest request
    ) {
        return ApiResponse.<UtilityBillingRunResponse>builder()
                .data(utilityBillingRunService.updateAdjustment(runId, itemId, request))
                .build();
    }

    @PostMapping("/{runId}/confirm")
    public ApiResponse<UtilityBillingRunResponse> confirmRun(@PathVariable Long runId) {
        return ApiResponse.<UtilityBillingRunResponse>builder()
                .data(utilityBillingRunService.confirmRun(runId))
                .build();
    }

    @PostMapping("/{runId}/generate-invoices")
    public ApiResponse<UtilityBillingRunResponse> generateInvoices(
            @PathVariable Long runId,
            @RequestParam(required = false) Integer dueDays
    ) {
        return ApiResponse.<UtilityBillingRunResponse>builder()
                .data(utilityBillingRunService.generateInvoices(
                        runId,
                        dueDays,
                        AuthUtils.getCurrentAuthenticationId()
                ))
                .build();
    }

    @PostMapping("/{runId}/publish")
    public ApiResponse<UtilityBillingRunResponse> publishBatch(
            @PathVariable Long runId,
            @RequestParam(required = false) Integer dueDays
    ) {
        return ApiResponse.<UtilityBillingRunResponse>builder()
                .data(utilityBillingRunService.publishBatch(
                        runId,
                        dueDays,
                        AuthUtils.getCurrentAuthenticationId()
                ))
                .build();
    }
}
