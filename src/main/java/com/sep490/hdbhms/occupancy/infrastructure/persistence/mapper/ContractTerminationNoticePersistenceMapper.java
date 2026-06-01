package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.ContractTerminationNotice;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractTerminationNoticeEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContractTerminationNoticePersistenceMapper {

    JpaLeaseContractRepository jpaLeaseContractRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;
    JpaUserRepository jpaUserRepository;

    public ContractTerminationNotice toDomain(ContractTerminationNoticeEntity entity) {
        if (entity == null) return null;
        return ContractTerminationNotice.builder()
                .id(entity.getId())
                .contractId(entity.getContract() != null ? entity.getContract().getId() : null)
                .noticeBy(entity.getNoticeBy())
                .noticeUserId(entity.getNoticeUser() != null ? entity.getNoticeUser().getId() : null)
                .noticeDate(entity.getNoticeDate())
                .expectedTerminationDate(entity.getExpectedTerminationDate())
                .reason(entity.getReason())
                .evidenceFileId(entity.getEvidenceFile() != null ? entity.getEvidenceFile().getId() : null)
                .status(entity.getStatus())
                .decidedById(entity.getDecidedBy() != null ? entity.getDecidedBy().getId() : null)
                .decidedAt(entity.getDecidedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public ContractTerminationNoticeEntity toEntity(ContractTerminationNotice domain) {
        if (domain == null) return null;
        return ContractTerminationNoticeEntity.builder()
                .id(domain.getId())
                .contract(domain.getContractId() != null
                        ? jpaLeaseContractRepository.findById(domain.getContractId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .noticeBy(domain.getNoticeBy())
                .noticeUser(domain.getNoticeUserId() != null
                        ? jpaUserRepository.findById(domain.getNoticeUserId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND))
                        : null)
                .noticeDate(domain.getNoticeDate())
                .expectedTerminationDate(domain.getExpectedTerminationDate())
                .reason(domain.getReason())
                .evidenceFile(domain.getEvidenceFileId() != null
                        ? jpaFileMetadataRepository.findById(domain.getEvidenceFileId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .status(domain.getStatus())
                .decidedBy(domain.getDecidedById() != null
                        ? jpaUserRepository.findById(domain.getDecidedById())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND))
                        : null)
                .decidedAt(domain.getDecidedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
