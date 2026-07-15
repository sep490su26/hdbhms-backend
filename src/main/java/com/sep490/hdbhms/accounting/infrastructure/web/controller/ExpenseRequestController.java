package com.sep490.hdbhms.accounting.infrastructure.web.controller;

import com.sep490.hdbhms.accounting.application.service.ExpenseRequestService;
import com.sep490.hdbhms.accounting.domain.value_objects.ExpenseStatus;
import com.sep490.hdbhms.accounting.domain.value_objects.ExpenseType;
import com.sep490.hdbhms.accounting.infrastructure.web.dto.request.CreateExpenseRequest;
import com.sep490.hdbhms.accounting.infrastructure.web.dto.request.MarkExpensePaidRequest;
import com.sep490.hdbhms.accounting.infrastructure.web.dto.request.RejectExpenseRequest;
import com.sep490.hdbhms.accounting.infrastructure.web.dto.response.ExpenseRequestResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/expense-requests")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpenseRequestController {

    ExpenseRequestService expenseRequestService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','ACCOUNTANT')")
    public ApiResponse<PageResponse<ExpenseRequestResponse>> listRequests(
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) ExpenseType expenseType,
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.<PageResponse<ExpenseRequestResponse>>builder()
                .code(0)
                .data(expenseRequestService.listRequests(
                        propertyId,
                        roomId,
                        expenseType,
                        status,
                        fromDate,
                        toDate,
                        keyword,
                        pageable
                ))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','ACCOUNTANT')")
    public ApiResponse<ExpenseRequestResponse> getRequest(@PathVariable Long id) {
        return ApiResponse.<ExpenseRequestResponse>builder()
                .code(0)
                .data(expenseRequestService.getRequest(id))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<ExpenseRequestResponse> createRequest(
            @Valid @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.<ExpenseRequestResponse>builder()
                .code(0)
                .data(expenseRequestService.createRequest(request, principal.getId(), principal.getRole()))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<ExpenseRequestResponse> cancelRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.<ExpenseRequestResponse>builder()
                .code(0)
                .data(expenseRequestService.cancelRequest(id, principal.getId(), principal.getRole()))
                .build();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<ExpenseRequestResponse> approveRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.<ExpenseRequestResponse>builder()
                .code(0)
                .data(expenseRequestService.approveRequest(id, principal.getId()))
                .build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<ExpenseRequestResponse> rejectRequest(
            @PathVariable Long id,
            @Valid @RequestBody RejectExpenseRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.<ExpenseRequestResponse>builder()
                .code(0)
                .data(expenseRequestService.rejectRequest(id, request, principal.getId()))
                .build();
    }

    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<ExpenseRequestResponse> markPaid(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) MarkExpensePaidRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.<ExpenseRequestResponse>builder()
                .code(0)
                .data(expenseRequestService.markPaid(id, request, principal.getId()))
                .build();
    }
}
