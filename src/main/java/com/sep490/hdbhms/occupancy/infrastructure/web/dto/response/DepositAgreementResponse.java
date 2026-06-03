package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositAgreementResponse {
    Long id;
    String depositCode;
    String roomCode;
    String propertyName;
    String depositorFullName;
    String depositorPhone;
    String depositorEmail;
    Long amount;
    LocalDateTime createdAt;
    DepositAgreementStatus status;
    LocalDateTime confirmedAt;
    Long contractFileId;
    String contractDownloadUrl;
}
