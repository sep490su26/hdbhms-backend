package com.sep490.hdbhms.billingandpayment.domain.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DebtSnapshot {
    Long id;
    Long roomId;
    Long contractId;
    LocalDate snapshotDate;
    Long rentDebtAmount;
    Long utilityDebtAmount;
    Long otherDebtAmount;
    Integer rentDebtMonths;
    Integer utilityDebtMonths;
    Long mixedDebtAmount;
    Long debtLimitAmount;
    Boolean isOverLimit;
    LocalDateTime createdAt;

    public static DebtSnapshot calculate(
            Long roomId,
            Long contractId,
            LocalDate date,
            long rentDebt,
            long utilityDebt,
            long otherDebt,
            int rentMonths,
            int utilityMonths,
            long mixedDebt,
            Long limit,
            boolean overLimit
    ) {
        return DebtSnapshot.builder()
                .roomId(roomId)
                .contractId(contractId)
                .snapshotDate(date)
                .rentDebtAmount(rentDebt)
                .utilityDebtAmount(utilityDebt)
                .otherDebtAmount(otherDebt)
                .rentDebtMonths(rentMonths)
                .utilityDebtMonths(utilityMonths)
                .mixedDebtAmount(mixedDebt)
                .debtLimitAmount(limit)
                .isOverLimit(overLimit)
                .build();
    }
}