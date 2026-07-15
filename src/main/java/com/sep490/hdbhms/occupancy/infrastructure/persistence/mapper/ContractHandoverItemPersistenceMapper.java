package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.ContractHandoverItem;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverItemEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractHandoverRecordRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomAssetRepository;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContractHandoverItemPersistenceMapper {

    JpaContractHandoverRecordRepository jpaContractHandoverRecordRepository;
    JpaRoomAssetRepository jpaRoomAssetRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;
    JpaInvoiceRepository jpaInvoiceRepository;

    public ContractHandoverItem toDomain(ContractHandoverItemEntity entity) {
        if (entity == null) return null;
        return ContractHandoverItem.builder()
                .id(entity.getId())
                .handoverRecordId(entity.getHandoverRecord() != null ? entity.getHandoverRecord().getId() : null)
                .roomAssetId(entity.getRoomAsset() != null ? entity.getRoomAsset().getId() : null)
                .assetName(entity.getAssetName())
                .quantity(entity.getQuantity())
                .conditionStatus(entity.getConditionStatus())
                .note(entity.getNote())
                .evidenceFileId(entity.getEvidenceFile() != null ? entity.getEvidenceFile().getId() : null)
                .compensationAmount(entity.getCompensationAmount())
                .compensationInvoiceId(entity.getCompensationInvoice() != null ? entity.getCompensationInvoice().getId() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public ContractHandoverItemEntity toEntity(ContractHandoverItem domain) {
        if (domain == null) return null;
        return ContractHandoverItemEntity.builder()
                .id(domain.getId())
                .handoverRecord(domain.getHandoverRecordId() != null
                        ? jpaContractHandoverRecordRepository.findById(domain.getHandoverRecordId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_HANDOVER_ITEM_NOT_FOUND))
                        : null)
                .roomAsset(domain.getRoomAssetId() != null
                        ? jpaRoomAssetRepository.findById(domain.getRoomAssetId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_HANDOVER_ITEM_NOT_FOUND))
                        : null)
                .assetName(domain.getAssetName())
                .quantity(domain.getQuantity())
                .conditionStatus(domain.getConditionStatus())
                .note(domain.getNote())
                .evidenceFile(domain.getEvidenceFileId() != null
                        ? jpaFileMetadataRepository.findById(domain.getEvidenceFileId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_HANDOVER_ITEM_NOT_FOUND))
                        : null)
                .compensationAmount(domain.getCompensationAmount())
                .compensationInvoice(domain.getCompensationInvoiceId() != null
                        ? jpaInvoiceRepository.findById(domain.getCompensationInvoiceId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_HANDOVER_ITEM_NOT_FOUND))
                        : null)
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
