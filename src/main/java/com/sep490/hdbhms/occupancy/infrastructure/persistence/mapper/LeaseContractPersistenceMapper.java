package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositAgreementRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaPersonProfileRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaseContractPersistenceMapper {

    JpaRoomRepository jpaRoomRepository;
    JpaDepositAgreementRepository jpaDepositAgreementRepository;
    JpaPersonProfileRepository jpaPersonProfileRepository;
    JpaLeaseContractRepository jpaLeaseContractRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;
    JpaUserRepository jpaUserRepository;

    public LeaseContract toDomain(LeaseContractEntity entity) {
        if (entity == null) return null;
        return LeaseContract.builder()
                .id(entity.getId())
                .contractCode(entity.getContractCode())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .depositAgreementId(entity.getDepositAgreement() != null ? entity.getDepositAgreement().getId() : null)
                .primaryTenantProfileId(entity.getPrimaryTenantProfile() != null ? entity.getPrimaryTenantProfile().getId() : null)
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .rentStartDate(entity.getRentStartDate())
                .monthlyRent(entity.getMonthlyRent())
                .paymentCycleMonths(entity.getPaymentCycleMonths())
                .depositAmount(entity.getDepositAmount())
                .status(entity.getStatus())
                .tenantIntention(entity.getTenantIntention())
                .expectedVacantDate(entity.getExpectedVacantDate())
                .intentionRecordedAt(entity.getIntentionRecordedAt())
                .previousContractId(entity.getPreviousContract() != null ? entity.getPreviousContract().getId() : null)
                .contractFileId(entity.getContractFile() != null ? entity.getContractFile().getId() : null)
                .signedFileId(entity.getSignedFile() != null ? entity.getSignedFile().getId() : null)
                .signedUploadedById(entity.getSignedUploadedBy() != null ? entity.getSignedUploadedBy().getId() : null)
                .signedAt(entity.getSignedAt())
                .createdById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .version(entity.getVersion())
                .build();
    }

    public LeaseContractEntity toEntity(LeaseContract domain) {
        if (domain == null) return null;
        return LeaseContractEntity.builder()
                .id(domain.getId())
                .contractCode(domain.getContractCode())
                .room(domain.getRoomId() != null
                        ? jpaRoomRepository.findById(domain.getRoomId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_NOT_FOUND))
                        : null)
                .depositAgreement(domain.getDepositAgreementId() != null
                        ? jpaDepositAgreementRepository.findById(domain.getDepositAgreementId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_NOT_FOUND))
                        : null)
                .primaryTenantProfile(domain.getPrimaryTenantProfileId() != null
                        ? jpaPersonProfileRepository.findById(domain.getPrimaryTenantProfileId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_NOT_FOUND))
                        : null)
                .startDate(domain.getStartDate())
                .endDate(domain.getEndDate())
                .rentStartDate(domain.getRentStartDate())
                .monthlyRent(domain.getMonthlyRent())
                .paymentCycleMonths(domain.getPaymentCycleMonths())
                .depositAmount(domain.getDepositAmount())
                .status(domain.getStatus())
                .tenantIntention(domain.getTenantIntention())
                .expectedVacantDate(domain.getExpectedVacantDate())
                .intentionRecordedAt(domain.getIntentionRecordedAt())
                .previousContract(domain.getPreviousContractId() != null
                        ? jpaLeaseContractRepository.findById(domain.getPreviousContractId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_NOT_FOUND))
                        : null)
                .contractFile(domain.getContractFileId() != null
                        ? jpaFileMetadataRepository.findById(domain.getContractFileId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_NOT_FOUND))
                        : null)
                .signedFile(domain.getSignedFileId() != null
                        ? jpaFileMetadataRepository.findById(domain.getSignedFileId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_NOT_FOUND))
                        : null)
                .signedUploadedBy(domain.getSignedUploadedById() != null
                        ? jpaUserRepository.findById(domain.getSignedUploadedById())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND))
                        : null)
                .signedAt(domain.getSignedAt())
                .createdBy(domain.getCreatedById() != null
                        ? jpaUserRepository.findById(domain.getCreatedById())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND))
                        : null)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .version(domain.getVersion())
                .build();
    }
}
