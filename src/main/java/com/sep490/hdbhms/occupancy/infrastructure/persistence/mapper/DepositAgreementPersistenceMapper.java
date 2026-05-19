package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaPersonProfileRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositAgreementEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositFormRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeadRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaTenantRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositAgreementPersistenceMapper {
    JpaRoomRepository jpaRoomRepository;
    JpaLeadRepository jpaLeadRepository;
    JpaTenantRepository jpaTenantRepository;
    JpaDepositFormRepository jpaDepositFormRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;
    JpaPersonProfileRepository jpaPersonProfileRepository;

    public DepositAgreement toDomain(DepositAgreementEntity entity) {
        if (entity == null) return null;
        return DepositAgreement.builder()
                .id(entity.getId())
                .depositCode(entity.getDepositCode())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .depositFormId(entity.getDepositForm() != null ? entity.getDepositForm().getId() : null)
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                .leadId(entity.getLead() != null ? entity.getLead().getId() : null)
                .depositorPersonProfileId(entity.getDepositorPersonProfile() != null ? entity.getDepositorPersonProfile().getId() : null)
                .amount(entity.getAmount())
                .expectedMoveInDate(entity.getExpectedMoveInDate())
                .expectedLeaseSignDate(entity.getExpectedLeaseSignDate())
                .paymentDueAt(entity.getPaymentDueAt())
                .depositExpiresAt(entity.getDepositExpiresAt())
                .extensionCount(entity.getExtensionCount())
                .maxExtensions(entity.getMaxExtensions())
                .status(entity.getStatus())
                .confirmedAt(entity.getConfirmedAt())
                .contractFileId(entity.getContractFile() != null ? entity.getContractFile().getId() : null)
                .note(entity.getNote())
                .forfeitureReason(entity.getForfeitureReason())
                .refundedAmount(entity.getRefundedAmount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public DepositAgreementEntity toEntity(DepositAgreement domain) {
        if (domain == null) return null;
        return DepositAgreementEntity.builder()
                .id(domain.getId())
                .depositCode(domain.getDepositCode())
                .room(domain.getRoomId() != null
                        ? jpaRoomRepository.findById(domain.getRoomId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .depositForm(domain.getDepositFormId() != null
                        ? jpaDepositFormRepository.findById(domain.getDepositFormId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .tenant(domain.getTenantId() != null
                        ? jpaTenantRepository.findById(domain.getTenantId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .lead(domain.getLeadId() != null
                        ? jpaLeadRepository.findById(domain.getLeadId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .depositorPersonProfile(domain.getDepositorPersonProfileId() != null
                        ? jpaPersonProfileRepository.findById(domain.getDepositorPersonProfileId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .amount(domain.getAmount())
                .expectedMoveInDate(domain.getExpectedMoveInDate())
                .expectedLeaseSignDate(domain.getExpectedLeaseSignDate())
                .paymentDueAt(domain.getPaymentDueAt())
                .depositExpiresAt(domain.getDepositExpiresAt())
                .extensionCount(domain.getExtensionCount())
                .maxExtensions(domain.getMaxExtensions())
                .status(domain.getStatus())
                .confirmedAt(domain.getConfirmedAt())
                .contractFile(domain.getContractFileId() != null
                        ? jpaFileMetadataRepository.findById(domain.getContractFileId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .note(domain.getNote())
                .forfeitureReason(domain.getForfeitureReason())
                .refundedAmount(domain.getRefundedAmount())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
