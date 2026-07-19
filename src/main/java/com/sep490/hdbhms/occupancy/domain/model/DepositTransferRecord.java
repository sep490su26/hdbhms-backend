package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.DepositTransferStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositTransferRecord {
    Long id;
    Long transferRequestId;
    Long oldContractId;
    Long newContractId;
    Long oldDepositAgreementId;
    Long fromRoomId;
    Long toRoomId;
    @Builder.Default
    Long amount = 0L;
    @Builder.Default
    DepositTransferStatus status = DepositTransferStatus.DRAFT;
    LocalDate effectiveDate;
    LocalDateTime cancelledAt;
    String note;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public void markEffective(LocalDate effectiveDate) {
        if (status == DepositTransferStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled deposit transfer records cannot become effective.");
        }
        this.status = DepositTransferStatus.EFFECTIVE;
        this.effectiveDate = effectiveDate;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (status == DepositTransferStatus.EFFECTIVE) {
            throw new IllegalStateException("Effective deposit transfer records cannot be cancelled.");
        }
        this.status = DepositTransferStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
