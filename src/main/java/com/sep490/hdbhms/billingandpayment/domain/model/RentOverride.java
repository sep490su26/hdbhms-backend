package com.sep490.hdbhms.billingandpayment.domain.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RentOverride {
    Long id;
    Long contractId;
    String billingPeriod;
    Long overrideMonthlyRent;
    String reason;
    Long approvedBy;
    LocalDateTime createdAt;

    public static RentOverride apply(
            Long contractId,
            String billingPeriod,
            Long newRent,
            String reason,
            Long approvedBy
    ) {
        return RentOverride.builder()
                .contractId(contractId)
                .billingPeriod(billingPeriod)
                .overrideMonthlyRent(newRent)
                .reason(reason)
                .approvedBy(approvedBy)
                .build();
    }
}