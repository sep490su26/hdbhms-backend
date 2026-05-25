package com.sep490.hdbhms.modules.mobile.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sep490.hdbhms.modules.auth.dto.OnboardingStateResponse;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MobileHomeResponse(
        UserSummary user,
        TenantSummary tenant,
        RoomSummary room,
        ContractSummary contract,

        @JsonProperty("invoice_summary")
        InvoiceSummary invoiceSummary,

        @JsonProperty("notification_summary")
        NotificationSummary notificationSummary,

        OnboardingStateResponse onboarding
) {
    public record UserSummary(
            Long id,

            @JsonProperty("full_name")
            String fullName,

            String phone,
            String email,
            String role
    ) {
    }

    public record TenantSummary(
            Long id,
            String name
    ) {
    }

    public record RoomSummary(
            Long id,

            @JsonProperty("room_code")
            String roomCode,

            String name,

            @JsonProperty("current_status")
            String currentStatus
    ) {
    }

    public record ContractSummary(
            Long id,

            @JsonProperty("contract_code")
            String contractCode,

            String status,

            @JsonProperty("start_date")
            LocalDate startDate,

            @JsonProperty("end_date")
            LocalDate endDate
    ) {
    }

    public record InvoiceSummary(
            @JsonProperty("unpaid_count")
            long unpaidCount,

            @JsonProperty("total_unpaid_amount")
            BigDecimal totalUnpaidAmount,

            @JsonProperty("nearest_due_date")
            LocalDate nearestDueDate
    ) {
    }

    public record NotificationSummary(
            @JsonProperty("unread_count")
            long unreadCount
    ) {
    }
}
