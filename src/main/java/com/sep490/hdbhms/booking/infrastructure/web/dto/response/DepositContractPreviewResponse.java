package com.sep490.hdbhms.booking.infrastructure.web.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositContractPreviewResponse {
    String html;
    String depositCode;
    Long depositAmount;
    String depositAmountText;
    String generatedAt;
}
