package com.sep490.hdbhms.billingandpayment.infrastructure.web.controller;

import com.sep490.hdbhms.billingandpayment.application.service.BillingManagementService;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.ApplyRentOverrideRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.ManualPaymentRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.BillingInvoiceResponse;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.ManualPaymentResponse;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.RentOverrideResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/admin/invoices")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class BillingManagementController {
    BillingManagementService billingManagementService;

    @GetMapping
    public ApiResponse<List<BillingInvoiceResponse>> listInvoices(
            @RequestParam(required = false) String billingPeriod,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String invoiceType
    ) {
        return ApiResponse.<List<BillingInvoiceResponse>>builder()
                .data(billingManagementService.listInvoices(billingPeriod, status, propertyId, roomId, invoiceType))
                .build();
    }

    @PostMapping("/rent-overrides")
    public ApiResponse<RentOverrideResponse> applyRentOverride(@RequestBody ApplyRentOverrideRequest request) {
        return ApiResponse.<RentOverrideResponse>builder()
                .data(billingManagementService.applyRentOverride(request, AuthUtils.getCurrentAuthenticationId()))
                .build();
    }

    @PostMapping("/{invoiceId}/manual-payments")
    public ApiResponse<ManualPaymentResponse> confirmManualPayment(
            @PathVariable Long invoiceId,
            @RequestBody ManualPaymentRequest request
    ) {
        return ApiResponse.<ManualPaymentResponse>builder()
                .data(billingManagementService.confirmManualPayment(invoiceId, request, AuthUtils.getCurrentAuthenticationId()))
                .build();
    }
}
