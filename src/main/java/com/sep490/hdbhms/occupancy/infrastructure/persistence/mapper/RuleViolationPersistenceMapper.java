package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.RuleViolation;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RuleViolationEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRuleRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaPersonProfileRepository;
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
public class RuleViolationPersistenceMapper {

    JpaPropertyRepository jpaPropertyRepository;
    JpaRoomRepository jpaRoomRepository;
    JpaLeaseContractRepository jpaLeaseContractRepository;
    JpaPersonProfileRepository jpaPersonProfileRepository;
    JpaPropertyRuleRepository jpaPropertyRuleRepository;
    JpaInvoiceRepository jpaInvoiceRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;
    JpaUserRepository jpaUserRepository;

    public RuleViolation toDomain(RuleViolationEntity entity) {
        if (entity == null) return null;
        return RuleViolation.builder()
                .id(entity.getId())
                .propertyId(entity.getProperty() != null ? entity.getProperty().getId() : null)
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .contractId(entity.getContract() != null ? entity.getContract().getId() : null)
                .tenantProfileId(entity.getTenantProfile() != null ? entity.getTenantProfile().getId() : null)
                .ruleId(entity.getRule() != null ? entity.getRule().getId() : null)
                .violationDate(entity.getViolationDate())
                .description(entity.getDescription())
                .fineAmount(entity.getFineAmount())
                .invoiceId(entity.getInvoice() != null ? entity.getInvoice().getId() : null)
                .evidenceFileId(entity.getEvidenceFile() != null ? entity.getEvidenceFile().getId() : null)
                .status(entity.getStatus())
                .createdById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public RuleViolationEntity toEntity(RuleViolation domain) {
        if (domain == null) return null;
        return RuleViolationEntity.builder()
                .id(domain.getId())
                .property(domain.getPropertyId() != null
                        ? jpaPropertyRepository.findById(domain.getPropertyId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .room(domain.getRoomId() != null
                        ? jpaRoomRepository.findById(domain.getRoomId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .contract(domain.getContractId() != null
                        ? jpaLeaseContractRepository.findById(domain.getContractId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .tenantProfile(domain.getTenantProfileId() != null
                        ? jpaPersonProfileRepository.findById(domain.getTenantProfileId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .rule(domain.getRuleId() != null
                        ? jpaPropertyRuleRepository.findById(domain.getRuleId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .violationDate(domain.getViolationDate())
                .description(domain.getDescription())
                .fineAmount(domain.getFineAmount())
                .invoice(domain.getInvoiceId() != null
                        ? jpaInvoiceRepository.findById(domain.getInvoiceId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .evidenceFile(domain.getEvidenceFileId() != null
                        ? jpaFileMetadataRepository.findById(domain.getEvidenceFileId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .status(domain.getStatus())
                .createdBy(domain.getCreatedById() != null
                        ? jpaUserRepository.findById(domain.getCreatedById())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND))
                        : null)
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
