package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverType;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverItemEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverRecordEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomAssetEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractHandoverItemRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractHandoverRecordRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomAssetRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.ContractHandoverDetailsResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        ContractHandoverRecordEntity record = handoverRecordRepository
                .findFirstByContract_IdAndHandoverTypeOrderByCreatedAtDesc(contractId, type)
                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_HANDOVER_RECORD_NOT_FOUND));

        List<ContractHandoverDetailsResponse.HandoverItemDetails> items =
                handoverItemRepository.findWithEvidenceFileByHandoverRecordId(record.getId()).stream()
                        .map(this::mapHandoverItem)
                        .toList();

        if (items.isEmpty()) {
            items = roomAssetRepository.findActiveByRoomId(roomId).stream()
                    .map(this::mapRoomAsset)
                    .toList();
        }

        return ContractHandoverDetailsResponse.builder()
                .handoverRecordId(record.getId())
                .handoverType(record.getHandoverType())
                .status(record.getStatus())
                .handoverDate(record.getHandoverDate())
                .note(record.getNote())
                .signedDocumentId(record.getSignedDocument() != null ? record.getSignedDocument().getId() : null)
                .signedDocumentUrl(fileDownloadUrl(record.getSignedDocument()))
                .electricity(mapReading(record.getElectricityReading()))
                .water(mapReading(record.getWaterReading()))
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
                .currentValue(reading.getCurrentValue())
                .readingDate(reading.getReadingDate().atStartOfDay())
                .photoFileId(reading.getPhotoFile() != null ? reading.getPhotoFile().getId() : null)
                .build();
    }

    private String fileDownloadUrl(FileMetadataEntity file) {
        return file == null ? null : "/api/v1/files/download/" + file.getId();
    }
}
