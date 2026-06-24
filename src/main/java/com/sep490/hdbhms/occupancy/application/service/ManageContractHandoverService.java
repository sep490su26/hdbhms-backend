package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverType;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterType;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingPurpose;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverRecordEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomAssetEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractHandoverRecordRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomAssetRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.ConfirmHandoverRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.HandoverMeterReadingsRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.SubmitHandoverRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.ContractHandoverDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.HandoverMeterReadingsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.SubmitHandoverResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManageContractHandoverService {

    JpaLeaseContractRepository leaseContractRepository;
    JpaMeterReadingRepository meterReadingRepository;
    JpaMeterRepository meterRepository;
    JpaContractHandoverRecordRepository handoverRecordRepository;
    JpaUserRepository userRepository;
    JpaFileMetadataRepository fileMetadataRepository;
    JpaRoomAssetRepository roomAssetRepository;

    @Transactional
    public HandoverMeterReadingsResponse createHandoverReadings(Long contractId, HandoverMeterReadingsRequest request, HandoverType handoverType) {
        LeaseContractEntity contract = leaseContractRepository.findByIdAndDeletedAtIsNull(contractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_NOT_FOUND));

        Long roomId = contract.getRoom().getId();

        // 1. Liên kết với Handover Record (tạo DRAFT nếu chưa có)
        ContractHandoverRecordEntity handoverRecord = handoverRecordRepository
                .findFirstByContract_IdAndHandoverTypeOrderByCreatedAtDesc(contractId, handoverType)
                .orElseGet(() -> ContractHandoverRecordEntity.builder()
                        .contract(contract)
                        .room(contract.getRoom())
                        .handoverType(handoverType)
                        .handoverDate(LocalDateTime.now())
                        .status(HandoverStatus.DRAFT)
                        .build());

        // 2. Tạo hoặc cập nhật Reading cho Điện
        MeterReadingEntity electricReading = createOrUpdateReading(contract.getRoom(), MeterType.ELECTRICITY, request.getElectricity(), handoverRecord.getElectricityReading());

        // 3. Tạo hoặc cập nhật Reading cho Nước
        MeterReadingEntity waterReading = createOrUpdateReading(contract.getRoom(), MeterType.WATER, request.getWater(), handoverRecord.getWaterReading());

        handoverRecord.setElectricityReading(electricReading);
        handoverRecord.setWaterReading(waterReading);
        
        handoverRecordRepository.save(handoverRecord);

        return HandoverMeterReadingsResponse.builder()
                .electricityReadingId(electricReading.getId())
                .waterReadingId(waterReading.getId())
                .build();
    }

    private MeterReadingEntity createOrUpdateReading(RoomEntity room, MeterType meterType, HandoverMeterReadingsRequest.ReadingInput input, MeterReadingEntity existingReading) {
        Long roomId = room.getId();

        var activeMeter = meterRepository.findFirstByRoom_IdAndMeterTypeAndStatus(roomId, meterType, com.sep490.hdbhms.occupancy.domain.value_objects.MeterStatus.ACTIVE)
                .orElseGet(() -> meterRepository.save(MeterEntity.builder()
                        .room(room)
                        .meterType(meterType)
                        .status(com.sep490.hdbhms.occupancy.domain.value_objects.MeterStatus.ACTIVE)
                        .installedAt(LocalDate.now())
                        .build()));

        if (existingReading != null) {
            existingReading.setCurrentValue(input.getCurrentValue());
            existingReading.setReadingDate(input.getReadingDate() != null ? input.getReadingDate() : LocalDate.now());
            if (input.getPhotoFileId() != null) {
                existingReading.setPhotoFile(fileMetadataRepository.getReferenceById(input.getPhotoFileId()));
            }
            return meterReadingRepository.save(existingReading);
        }

        var latestReadingOpt = meterReadingRepository.findFirstByRoom_IdAndMeter_MeterTypeOrderByReadingDateDesc(roomId, meterType);
        BigDecimal prevValue = latestReadingOpt.map(MeterReadingEntity::getCurrentValue).orElse(BigDecimal.ZERO);

        String currentPeriod = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/yyyy"));
        
        int nextRevision = 1;
        var existingPeriodReadingOpt = meterReadingRepository.findFirstByMeter_IdAndReadingPeriodOrderByRevisionNoDesc(activeMeter.getId(), currentPeriod);
        if (existingPeriodReadingOpt.isPresent()) {
            nextRevision = existingPeriodReadingOpt.get().getRevisionNo() + 1;
        }

        MeterReadingEntity reading = MeterReadingEntity.builder()
                .meter(activeMeter)
                .room(activeMeter.getRoom())
                .readingPeriod(currentPeriod)
                .revisionNo(nextRevision)
                .previousValue(prevValue)
                .currentValue(input.getCurrentValue())
                .readingDate(input.getReadingDate() != null ? input.getReadingDate() : LocalDate.now())
                .purpose(ReadingPurpose.HANDOVER)
                .status(ReadingStatus.CONFIRMED)
                .createdBy(userRepository.getReferenceById(AuthUtils.getCurrentAuthenticationId()))
                .build();

        if (input.getPhotoFileId() != null) {
            reading.setPhotoFile(fileMetadataRepository.getReferenceById(input.getPhotoFileId()));
        }

        return meterReadingRepository.save(reading);
    }

    @Transactional
    public void confirmHandover(Long contractId, ConfirmHandoverRequest request) {
        ContractHandoverRecordEntity handoverRecord = handoverRecordRepository
                .findFirstByContract_IdAndHandoverTypeOrderByCreatedAtDesc(contractId, request.getHandoverType())
                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_HANDOVER_RECORD_NOT_FOUND));

        if (handoverRecord.getStatus() == HandoverStatus.CONFIRMED) {
            throw new AppException(ApiErrorCode.HANDOVER_001);
        }

        handoverRecord.setStatus(HandoverStatus.CONFIRMED);
        handoverRecord.setNote(request.getNote());
        handoverRecord.setConfirmedBy(userRepository.getReferenceById(AuthUtils.getCurrentAuthenticationId()));
        handoverRecord.setConfirmedAt(LocalDateTime.now());

        handoverRecordRepository.save(handoverRecord);
    }

    /**
     * Single-shot submit: saves meter readings + room assets + confirms the handover record
     * in a single transaction.
     *
     * POST /api/v1/lease-contracts/{contractId}/handover/submit
     */
    @Transactional
    public SubmitHandoverResponse submitHandover(Long contractId, SubmitHandoverRequest request) {
        LeaseContractEntity contract = leaseContractRepository.findByIdAndDeletedAtIsNull(contractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_NOT_FOUND));

        Long roomId = contract.getRoom().getId();
        HandoverType handoverType = request.getHandoverType();

        // ── 1. Handover record (create or update existing DRAFT) ─────────────
        ContractHandoverRecordEntity record = handoverRecordRepository
                .findFirstByContract_IdAndHandoverTypeOrderByCreatedAtDesc(contractId, handoverType)
                .orElseGet(() -> ContractHandoverRecordEntity.builder()
                        .contract(contract)
                        .room(contract.getRoom())
                        .handoverType(handoverType)
                        .build());

        if (record.getStatus() == HandoverStatus.CONFIRMED) {
            throw new AppException(ApiErrorCode.HANDOVER_001);
        }

        // ── 2. Meter readings ────────────────────────────────────────────────
        MeterReadingEntity electricReading = createOrUpdateReading(contract.getRoom(), MeterType.ELECTRICITY, toReadingInput(request.getElectricity()), record.getElectricityReading());
        MeterReadingEntity waterReading   = createOrUpdateReading(contract.getRoom(), MeterType.WATER,        toReadingInput(request.getWater()), record.getWaterReading());

        LocalDateTime handoverDateTime = request.getHandoverDate() != null
                ? request.getHandoverDate().atStartOfDay()
                : LocalDateTime.now();

        record.setHandoverDate(handoverDateTime);
        record.setElectricityReading(electricReading);
        record.setWaterReading(waterReading);
        record.setNote(request.getNote());
        record.setStatus(HandoverStatus.CONFIRMED);
        record.setConfirmedBy(userRepository.getReferenceById(AuthUtils.getCurrentAuthenticationId()));
        record.setConfirmedAt(LocalDateTime.now());
        record = handoverRecordRepository.save(record);

        // ── 3. Room assets (upsert) ──────────────────────────────────────────
        List<SubmitHandoverResponse.AssetResult> assetResults = new ArrayList<>();
        if (request.getAssets() != null) {
            for (SubmitHandoverRequest.AssetInput input : request.getAssets()) {
                boolean isNew = (input.getId() == null);
                RoomAssetEntity entity;

                if (isNew) {
                    entity = new RoomAssetEntity();
                    entity.setRoom(contract.getRoom());
                } else {
                    entity = roomAssetRepository
                            .findByIdAndRoom_IdAndDeletedAtIsNull(input.getId(), roomId)
                            .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_ASSET_NOT_FOUND));
                }

                entity.setAssetName(input.getAssetName().trim());
                entity.setAssetCategory(input.getAssetCategory().trim());
                entity.setQuantity(input.getQuantity());
                entity.setCurrentCondition(input.getCurrentCondition());
                entity.setDescription(input.getDescription() != null ? input.getDescription().trim() : null);

                if (input.getFileImageId() != null) {
                    entity.setImageFile(fileMetadataRepository.getReferenceById(input.getFileImageId()));
                } else if (isNew) {
                    entity.setImageFile(null);
                }
                // If updating and no new fileImageId → keep existing image

                entity = roomAssetRepository.save(entity);
                assetResults.add(SubmitHandoverResponse.AssetResult.builder()
                        .id(entity.getId())
                        .assetName(entity.getAssetName())
                        .created(isNew)
                        .build());
            }
        }

        return SubmitHandoverResponse.builder()
                .handoverRecordId(record.getId())
                .handoverType(record.getHandoverType())
                .status(record.getStatus())
                .handoverDate(record.getHandoverDate())
                .electricityReadingId(electricReading.getId())
                .waterReadingId(waterReading.getId())
                .assets(assetResults)
                .build();
    }

    @Transactional(readOnly = true)
    public ContractHandoverDetailsResponse getHandoverDetails(Long contractId, HandoverType type) {
        ContractHandoverRecordEntity record = handoverRecordRepository
                .findFirstByContract_IdAndHandoverTypeOrderByCreatedAtDesc(contractId, type)
                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_HANDOVER_RECORD_NOT_FOUND));

        return ContractHandoverDetailsResponse.builder()
                .handoverRecordId(record.getId())
                .handoverType(record.getHandoverType())
                .status(record.getStatus())
                .handoverDate(record.getHandoverDate())
                .note(record.getNote())
                .signedDocumentId(record.getSignedDocument() != null ? record.getSignedDocument().getId() : null)
                .signedDocumentUrl(record.getSignedDocument() != null ? "/api/v1/files/" + record.getSignedDocument().getId() : null)
                .electricity(mapReading(record.getElectricityReading()))
                .water(mapReading(record.getWaterReading()))
                .build();
    }

    private ContractHandoverDetailsResponse.MeterReadingDetails mapReading(MeterReadingEntity r) {
        if (r == null) return null;
        return ContractHandoverDetailsResponse.MeterReadingDetails.builder()
                .id(r.getId())
                .currentValue(r.getCurrentValue())
                .readingDate(r.getReadingDate().atStartOfDay())
                .photoFileId(r.getPhotoFile() != null ? r.getPhotoFile().getId() : null)
                .build();
    }

    /** Converts the SubmitHandoverRequest.MeterInput to the shared ReadingInput format */
    private HandoverMeterReadingsRequest.ReadingInput toReadingInput(SubmitHandoverRequest.MeterInput src) {
        HandoverMeterReadingsRequest.ReadingInput out = new HandoverMeterReadingsRequest.ReadingInput();
        out.setCurrentValue(src.getCurrentValue());
        out.setPhotoFileId(src.getPhotoFileId());
        out.setReadingDate(src.getReadingDate());
        return out;
    }
}
