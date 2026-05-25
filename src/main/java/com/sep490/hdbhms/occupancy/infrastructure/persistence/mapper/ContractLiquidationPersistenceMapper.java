package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.ContractLiquidation;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractLiquidationEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
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
public class ContractLiquidationPersistenceMapper {

    JpaLeaseContractRepository jpaLeaseContractRepository;
    JpaInvoiceRepository jpaInvoiceRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;
    JpaUserRepository jpaUserRepository;

    public ContractLiquidation toDomain(ContractLiquidationEntity entity) {
        if (entity == null) return null;
        return ContractLiquidation.builder()
                .id(entity.getId())
                .contractId(entity.getContract() != null ? entity.getContract().getId() : null)
                .liquidationDate(entity.getLiquidationDate())
                .reason(entity.getReason())
                .depositAmount(entity.getDepositAmount())
                .depositDeductionAmount(entity.getDepositDeductionAmount())
                .depositDeductionReason(entity.getDepositDeductionReason())
                .depositRefundAmount(entity.getDepositRefundAmount())
                .finalInvoiceId(entity.getFinalInvoice() != null ? entity.getFinalInvoice().getId() : null)
                .signedFileId(entity.getSignedFile() != null ? entity.getSignedFile().getId() : null)
                .status(entity.getStatus())
                .createdById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public ContractLiquidationEntity toEntity(ContractLiquidation domain) {
        if (domain == null) return null;
        return ContractLiquidationEntity.builder()
                .id(domain.getId())
                .contract(domain.getContractId() != null
                        ? jpaLeaseContractRepository.findById(domain.getContractId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .liquidationDate(domain.getLiquidationDate())
                .reason(domain.getReason())
                .depositAmount(domain.getDepositAmount())
                .depositDeductionAmount(domain.getDepositDeductionAmount())
                .depositDeductionReason(domain.getDepositDeductionReason())
                .depositRefundAmount(domain.getDepositRefundAmount())
                .finalInvoice(domain.getFinalInvoiceId() != null
                        ? jpaInvoiceRepository.findById(domain.getFinalInvoiceId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .signedFile(domain.getSignedFileId() != null
                        ? jpaFileMetadataRepository.findById(domain.getSignedFileId())
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
