package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BalanceResponse {
    int coinBalance;
    Instant createdAt;
    Instant lastUpdatedAt;
}
