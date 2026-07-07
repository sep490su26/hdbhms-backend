package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request;

public record ApplyRentOverrideRequest(
        Long roomId,
        String billingPeriod,
        Long overrideMonthlyRent,
        String reason
) {
}
