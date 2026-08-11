package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
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
public class LeaseContract {
    Long id;
    String contractCode;
    Long roomId;
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
    String tenantIntention;
    LocalDate expectedVacantDate;
    LocalDateTime intentionRecordedAt;
    Long previousContractId;
    Long contractFileId;
    Long signedFileId;
    Long signedUploadedById;
    LocalDateTime signedAt;
    Long createdById;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;
    @Builder.Default
    Long version = 0L;

    public static LeaseContract newLeaseContract(
            Long roomId,
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
        if (this.status != LeaseStatus.DRAFT && this.status != LeaseStatus.SIGNED) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE);
        }
        this.status = LeaseStatus.ACTIVE;
        if (this.signedAt == null) {
            this.signedAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void confirmContract() {
        if (this.status != LeaseStatus.DRAFT) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE);
        }
        this.status = LeaseStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now();
    }

    public void signContract() {
        if (this.status != LeaseStatus.CONFIRMED && this.status != LeaseStatus.PENDING_SIGNATURE) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE);
        }
        this.status = LeaseStatus.SIGNED;
        this.signedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void cancelContract() {
        if (this.status == LeaseStatus.ACTIVE) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE);
        }
        this.status = LeaseStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markTransferred() {
        if (this.status == LeaseStatus.TRANSFERRED) {
            return;
        }
        if (this.status != LeaseStatus.ACTIVE
                && this.status != LeaseStatus.EXPIRING_SOON
                && this.status != LeaseStatus.TERMINATION_PENDING) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE);
        }
        this.status = LeaseStatus.TRANSFERRED;
        this.updatedAt = LocalDateTime.now();
    }

    public void attachSignedFile(Long signedFileId, Long uploadedById) {
        this.signedFileId = signedFileId;
        this.signedUploadedById = uploadedById;
        this.updatedAt = LocalDateTime.now();
    }
}
