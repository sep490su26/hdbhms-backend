package com.sep490.hdbhms.booking.domain.model;

import com.sep490.hdbhms.booking.domain.value_objects.DepositTransferStatus;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
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
            throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE);
        }
        this.status = DepositTransferStatus.EFFECTIVE;
        this.effectiveDate = effectiveDate;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (status == DepositTransferStatus.EFFECTIVE) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE);
        }
        this.status = DepositTransferStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
