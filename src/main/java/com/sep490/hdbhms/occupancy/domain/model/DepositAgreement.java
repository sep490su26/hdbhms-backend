package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositAgreement {
    Long id;
    String depositCode;
    Long roomId;
    Long tenantId;
    Long leadId;
    Long depositorPersonProfileId;
    Long amount;
    LocalDate expectedMoveInDate;
    LocalDate expectedLeaseSignDate;
    LocalDateTime paymentDueAt;
    LocalDate depositExpiresAt;
    @Builder.Default
    Integer extensionCount = 0;
    @Builder.Default
    Integer maxExtensions = 1;
    @Builder.Default
    DepositAgreementStatus status = DepositAgreementStatus.PENDING_PAYMENT;
    LocalDateTime confirmedAt;
    Long contractFileId;
    String note;
    String forfeitureReason;
    Long refundedAmount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static DepositAgreement newDepositAgreementForLeadUser(
            String depositCode,
            Long roomId,
            Long leadId,
            Long depositorPersonProfileId,
            Long amount,
            LocalDate expectedMoveInDate,
            LocalDate expectedLeaseSignDate
    ) {
        return DepositAgreement.builder()
                .depositCode(depositCode)
                .roomId(roomId)
                .leadId(leadId)
                .depositorPersonProfileId(depositorPersonProfileId)
                .amount(amount)
                .expectedMoveInDate(expectedMoveInDate)
                .expectedLeaseSignDate(expectedLeaseSignDate)
                .build();
    }
}
