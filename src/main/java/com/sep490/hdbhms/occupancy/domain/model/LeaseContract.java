package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeaseContract {
    Long id;
    String contractCode;
    Long roomId;
    Long depositAgreementId;
    Long primaryTenantProfileId;
    LocalDate startDate;
    LocalDate endDate;
    LocalDate rentStartDate;
    Long monthlyRent;
    Integer paymentCycleMonths;
    @Builder.Default
    Long depositAmount = 0L;
    @Builder.Default
    LeaseStatus status = LeaseStatus.DRAFT;
    Long previousContractId;
    Long contractFileId;
    LocalDateTime signedAt;
    Long createdById;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;
    @Builder.Default
    Long version = 0L;

    public static LeaseContract newLeaseContract(
            Long roomId,
            Long depositAgreementId,
            Long primaryTenantProfileId,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate rentStartDate,
            Long monthlyRent,
            Integer paymentCycleMonths,
            Long depositAmount
    ) {
        return LeaseContract.builder()
                .roomId(roomId)
                .depositAgreementId(depositAgreementId)
                .primaryTenantProfileId(primaryTenantProfileId)
                .startDate(startDate)
                .endDate(endDate)
                .rentStartDate(rentStartDate)
                .monthlyRent(monthlyRent)
                .paymentCycleMonths(paymentCycleMonths)
                .depositAmount(depositAmount)
                .build();
    }

    public void activateContract() {
        if (this.status != LeaseStatus.DRAFT) {
            throw new IllegalStateException("Lease contract is not a draft.");
        }
        this.status = LeaseStatus.ACTIVE;
        if (this.signedAt == null) {
            this.signedAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }
}
