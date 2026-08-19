package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverType;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverItemEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverRecordEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomAssetEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractHandoverItemRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractHandoverRecordRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomAssetRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.ContractHandoverDetailsResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantHandoverService {

    JpaLeaseContractRepository leaseContractRepository;
    JpaContractHandoverRecordRepository handoverRecordRepository;
    JpaContractHandoverItemRepository handoverItemRepository;
    JpaRoomAssetRepository roomAssetRepository;
    LeaseContractQueryService leaseContractQueryService;

    @Transactional(readOnly = true)
    public ContractHandoverDetailsResponse getHandoverItems(Long contractId, HandoverType type) {
        LeaseContractEntity contract = leaseContractRepository.findByIdAndDeletedAtIsNull(contractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_NOT_FOUND));
        Long roomId = contract.getRoom().getId();

        leaseContractQueryService.assertCurrentUserCanReadContract(contractId);
        leaseContractQueryService.assertCurrentUserCanReadRoom(roomId);

        Optional<ContractHandoverRecordEntity> record = handoverRecordRepository
                .findFirstByContract_IdAndHandoverTypeOrderByCreatedAtDesc(contractId, type);

        List<ContractHandoverDetailsResponse.HandoverItemDetails> items = record
                .map(handover -> handoverItemRepository.findWithEvidenceFileByHandoverRecordId(handover.getId()).stream()
                        .map(this::mapHandoverItem)
                        .toList())
                .filter(handoverItems -> !handoverItems.isEmpty())
                .orElseGet(() -> roomAssetRepository.findActiveByRoomId(roomId).stream()
                        .map(this::mapRoomAsset)
                        .toList());

        return ContractHandoverDetailsResponse.builder()
                .handoverRecordId(record.map(ContractHandoverRecordEntity::getId).orElse(null))
                .handoverType(record.map(ContractHandoverRecordEntity::getHandoverType).orElse(null))
                .status(record.map(ContractHandoverRecordEntity::getStatus).orElse(null))
                .handoverDate(record.map(ContractHandoverRecordEntity::getHandoverDate).orElse(null))
                .note(record.map(ContractHandoverRecordEntity::getNote).orElse(null))
                .signedDocumentId(record.map(ContractHandoverRecordEntity::getSignedDocument)
                        .map(FileMetadataEntity::getId).orElse(null))
                .signedDocumentUrl(record.map(ContractHandoverRecordEntity::getSignedDocument)
                        .map(this::fileDownloadUrl).orElse(null))
                .electricity(record.map(ContractHandoverRecordEntity::getElectricityReading)
                        .map(this::mapReading).orElse(null))
                .items(items)
                .build();
    }

    private ContractHandoverDetailsResponse.HandoverItemDetails mapHandoverItem(
            ContractHandoverItemEntity item
    ) {
        FileMetadataEntity evidenceFile = item.getEvidenceFile();
        return ContractHandoverDetailsResponse.HandoverItemDetails.builder()
                .id(item.getId())
                .assetName(item.getAssetName())
                .quantity(item.getQuantity())
                .conditionStatus(item.getConditionStatus())
                .note(item.getNote())
                .evidenceFileId(evidenceFile != null ? evidenceFile.getId() : null)
                .evidenceFileUrl(fileDownloadUrl(evidenceFile))
                .compensationAmount(item.getCompensationAmount())
                .compensationInvoiceId(item.getCompensationInvoice() != null ? item.getCompensationInvoice().getId() : null)
                .build();
    }

    private ContractHandoverDetailsResponse.HandoverItemDetails mapRoomAsset(RoomAssetEntity asset) {
        FileMetadataEntity imageFile = asset.getImageFile();
        return ContractHandoverDetailsResponse.HandoverItemDetails.builder()
                .id(asset.getId())
                .assetName(asset.getAssetName())
                .quantity(asset.getQuantity())
                .conditionStatus(asset.getCurrentCondition())
                .note(asset.getDescription())
                .evidenceFileId(imageFile != null ? imageFile.getId() : null)
                .evidenceFileUrl(fileDownloadUrl(imageFile))
                .build();
    }

    private ContractHandoverDetailsResponse.MeterReadingDetails mapReading(MeterReadingEntity reading) {
        if (reading == null) {
            return null;
        }
        return ContractHandoverDetailsResponse.MeterReadingDetails.builder()
                .id(reading.getId())
                .previousValue(reading.getPreviousValue())
                .currentValue(reading.getCurrentValue())
                .readingDate(reading.getReadingDate().atStartOfDay())
                .photoFileId(reading.getPhotoFile() != null ? reading.getPhotoFile().getId() : null)
                .build();
    }

    private String fileDownloadUrl(FileMetadataEntity file) {
        return file == null ? null : "/api/v1/files/download/" + file.getId();
    }
}
