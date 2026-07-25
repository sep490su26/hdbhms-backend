package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceReason;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.occupancy.domain.value_objects.AssetCondition;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverType;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterType;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingPurpose;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverRecordEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverItemEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomAssetEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractHandoverRecordRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractHandoverItemRepository;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManageContractHandoverService {

    JpaLeaseContractRepository leaseContractRepository;
    JpaMeterReadingRepository meterReadingRepository;
    JpaMeterRepository meterRepository;
    JpaContractHandoverRecordRepository handoverRecordRepository;
    JpaContractHandoverItemRepository handoverItemRepository;
    JpaUserRepository userRepository;
    JpaFileMetadataRepository fileMetadataRepository;
    JpaRoomAssetRepository roomAssetRepository;
    JpaInvoiceRepository invoiceRepository;
    JpaInvoiceLineRepository invoiceLineRepository;
    JdbcTemplate jdbcTemplate;

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
        LocalDate readingDate = input.getReadingDate() != null ? input.getReadingDate() : LocalDate.now();

        if (existingReading != null) {
            existingReading.setCurrentValue(input.getCurrentValue());
            existingReading.setReadingDate(readingDate);
            if (input.getPhotoFileId() != null) {
                existingReading.setPhotoFile(fileMetadataRepository.getReferenceById(input.getPhotoFileId()));
            }
            return meterReadingRepository.save(existingReading);
        }

        var latestReadingOpt = meterReadingRepository
                .findFirstByRoom_IdAndMeter_MeterTypeAndStatusNotOrderByReadingDateDescCreatedAtDescIdDesc(
                        roomId,
                        meterType,
                        ReadingStatus.VOIDED
                );
        BigDecimal prevValue = latestReadingOpt.map(MeterReadingEntity::getCurrentValue).orElse(BigDecimal.ZERO);

        String currentPeriod = MeterReadingPeriod.from(readingDate);
        
        int nextRevision = 1;
        var existingPeriodReadingOpt = meterReadingRepository.findFirstByMeter_IdAndReadingPeriodOrderByRevisionNoDesc(activeMeter.getId(), currentPeriod);
        if (existingPeriodReadingOpt.isPresent()) {
            MeterReadingEntity existingPeriodReading = existingPeriodReadingOpt.get();
            nextRevision = existingPeriodReading.getRevisionNo() + 1;
            if (existingPeriodReading.getStatus() != ReadingStatus.VOIDED) {
                existingPeriodReading.setStatus(ReadingStatus.VOIDED);
                existingPeriodReading.setVoidReason("Superseded by handover reading revision " + nextRevision);
                meterReadingRepository.saveAndFlush(existingPeriodReading);
            }
        }

        MeterReadingEntity reading = MeterReadingEntity.builder()
                .meter(activeMeter)
                .room(activeMeter.getRoom())
                .readingPeriod(currentPeriod)
                .revisionNo(nextRevision)
                .previousValue(prevValue)
                .currentValue(input.getCurrentValue())
                .readingDate(readingDate)
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
        if (handoverType == HandoverType.MOVE_OUT) {
            requireNoUnpaidLeaseInvoices(
                    contractId,
                    "Khach thue can thanh toan het hoa don con no truoc khi ban giao tra phong."
            );
        }

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
        if (handoverType == HandoverType.TRANSFER_OUT || handoverType == HandoverType.TRANSFER_IN) {
            electricReading.setPurpose(ReadingPurpose.TRANSFER);
            waterReading.setPurpose(ReadingPurpose.TRANSFER);
            electricReading = meterReadingRepository.save(electricReading);
            waterReading = meterReadingRepository.save(waterReading);
        }

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
        List<ContractHandoverItemEntity> damageItems = new ArrayList<>();
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
                if (handoverType == HandoverType.MOVE_OUT && isDamageItem(input)) {
                    damageItems.add(toDamageItem(record, entity, input));
                }
                assetResults.add(SubmitHandoverResponse.AssetResult.builder()
                        .id(entity.getId())
                        .assetName(entity.getAssetName())
                        .created(isNew)
                        .build());
            }
        }
        softDeleteAssets(roomId, request.getDeletedAssetIds());
        InvoiceEntity compensationInvoice = createMoveOutCompensationInvoiceIfNeeded(contract, record, damageItems);

        return SubmitHandoverResponse.builder()
                .handoverRecordId(record.getId())
                .handoverType(record.getHandoverType())
                .status(record.getStatus())
                .handoverDate(record.getHandoverDate())
                .electricityReadingId(electricReading.getId())
                .waterReadingId(waterReading.getId())
                .assets(assetResults)
                .compensationInvoiceId(compensationInvoice == null ? null : compensationInvoice.getId())
                .compensationAmount(compensationInvoice == null ? 0L : compensationInvoice.getTotalAmount())
                .build();
    }

    private boolean isDamageItem(SubmitHandoverRequest.AssetInput input) {
        if (input == null) {
            return false;
        }
        long compensationAmount = input.getCompensationAmount() == null ? 0L : input.getCompensationAmount();
        return compensationAmount > 0
                || input.getCurrentCondition() == AssetCondition.BROKEN
                || input.getCurrentCondition() == AssetCondition.MISSING
                || hasText(input.getDamageNote());
    }

    private ContractHandoverItemEntity toDamageItem(
            ContractHandoverRecordEntity record,
            RoomAssetEntity roomAsset,
            SubmitHandoverRequest.AssetInput input
    ) {
        Long evidenceFileId = input.getEvidenceFileId();
        return ContractHandoverItemEntity.builder()
                .handoverRecord(record)
                .roomAsset(roomAsset)
                .assetName(input.getAssetName().trim())
                .quantity(input.getQuantity() == null ? 1 : input.getQuantity())
                .conditionStatus(input.getCurrentCondition() == null ? AssetCondition.GOOD : input.getCurrentCondition())
                .note(firstText(input.getDamageNote(), input.getDescription()))
                .evidenceFile(evidenceFileId == null ? null : fileMetadataRepository.getReferenceById(evidenceFileId))
                .compensationAmount(Math.max(0L, input.getCompensationAmount() == null ? 0L : input.getCompensationAmount()))
                .build();
    }

    private InvoiceEntity createMoveOutCompensationInvoiceIfNeeded(
            LeaseContractEntity contract,
            ContractHandoverRecordEntity record,
            List<ContractHandoverItemEntity> damageItems
    ) {
        if (damageItems.isEmpty()) {
            return null;
        }
        damageItems = handoverItemRepository.saveAll(damageItems);

        long totalAmount = damageItems.stream()
                .mapToLong(item -> item.getCompensationAmount() == null ? 0L : item.getCompensationAmount())
                .sum();
        if (totalAmount <= 0) {
            return null;
        }

        LocalDate handoverDate = record.getHandoverDate() == null
                ? LocalDate.now()
                : record.getHandoverDate().toLocalDate();
        String billingPeriod = YearMonth.from(handoverDate).toString();
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = AuthUtils.getCurrentAuthenticationId();

        InvoiceEntity invoice = invoiceRepository.save(InvoiceEntity.builder()
                .invoiceCode("INV-HO-COMP-" + record.getId() + "-" + now.toString().replace(":", "").replace("-", "").replace("T", "").replace(".", ""))
                .property(contract.getRoom().getProperty())
                .room(contract.getRoom())
                .leastContract(contract)
                .invoiceType(InvoiceType.COMPENSATION)
                .invoiceReason(InvoiceReason.ROOM_CLOSE)
                .revisionNo(nextInvoiceRevision(contract.getId(), billingPeriod, InvoiceType.COMPENSATION))
                .billingPeriod(billingPeriod)
                .issueDate(now)
                .dueDate(now.plusDays(7))
                .status(InvoiceStatus.ISSUED)
                .subtotalAmount(totalAmount)
                .discountAmount(0L)
                .totalAmount(totalAmount)
                .paidAmount(0L)
                .remainingAmount(totalAmount)
                .createdBy(currentUserId == null ? null : userRepository.getReferenceById(currentUserId))
                .issuedAt(now)
                .build());

        for (ContractHandoverItemEntity item : damageItems) {
            long amount = item.getCompensationAmount() == null ? 0L : item.getCompensationAmount();
            if (amount <= 0) {
                continue;
            }
            invoiceLineRepository.save(InvoiceLineEntity.builder()
                    .invoice(invoice)
                    .lineType(InvoiceLineType.MAINTENANCE_COMPENSATION)
                    .description(buildDamageDescription(item))
                    .quantity(1)
                    .unitPrice(amount)
                    .sourceType("CONTRACT_HANDOVER_DAMAGE")
                    .sourceId(item.getId())
                    .build());
            item.setCompensationInvoice(invoice);
        }
        handoverItemRepository.saveAll(damageItems);
        return invoiceRepository.save(invoice);
    }

    private int nextInvoiceRevision(Long contractId, String billingPeriod, InvoiceType invoiceType) {
        Integer maxRevision = jdbcTemplate.queryForObject("""
                        SELECT COALESCE(MAX(revision_no), 0)
                        FROM invoices
                        WHERE lease_contract_id = ?
                          AND billing_period = ?
                          AND invoice_type = ?
                        """,
                Integer.class,
                contractId,
                billingPeriod,
                invoiceType.name()
        );
        return (maxRevision == null ? 0 : maxRevision) + 1;
    }

    private void requireNoUnpaidLeaseInvoices(Long contractId, String message) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM invoices
                        WHERE lease_contract_id = ?
                          AND status NOT IN ('PAID', 'VOIDED')
                          AND remaining_amount > 0
                        """,
                Integer.class,
                contractId
        );
        if (count != null && count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    private String buildDamageDescription(ContractHandoverItemEntity item) {
        String suffix = hasText(item.getNote()) ? ": " + item.getNote().trim() : "";
        String description = "Boi thuong thiet hai khi ban giao tra phong - " + item.getAssetName() + suffix;
        return description.length() <= 1000 ? description : description.substring(0, 1000);
    }

    private String firstText(String first, String second) {
        if (hasText(first)) {
            return first.trim();
        }
        return hasText(second) ? second.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    void softDeleteAssets(Long roomId, List<Long> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return;
        }

        LocalDateTime deletedAt = LocalDateTime.now();
        for (Long assetId : new LinkedHashSet<>(assetIds)) {
            RoomAssetEntity entity = roomAssetRepository
                    .findByIdAndRoom_IdAndDeletedAtIsNull(assetId, roomId)
                    .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_ASSET_NOT_FOUND));
            entity.setDeletedAt(deletedAt);
            roomAssetRepository.save(entity);
        }
    }

    @Transactional(readOnly = true)
    public ContractHandoverDetailsResponse getHandoverDetails(Long contractId, HandoverType type) {
        return findHandoverDetails(contractId, type)
                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_HANDOVER_RECORD_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Optional<ContractHandoverDetailsResponse> findHandoverDetails(Long contractId, HandoverType type) {
        return handoverRecordRepository
                .findFirstByContract_IdAndHandoverTypeOrderByCreatedAtDesc(contractId, type)
                .map(this::toHandoverDetails);
    }

    private ContractHandoverDetailsResponse toHandoverDetails(ContractHandoverRecordEntity record) {
        List<ContractHandoverDetailsResponse.HandoverItemDetails> items =
                handoverItemRepository.findWithEvidenceFileByHandoverRecordId(record.getId()).stream()
                        .map(this::mapHandoverItem)
                        .toList();
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
                .items(items)
                .build();
    }

    private ContractHandoverDetailsResponse.HandoverItemDetails mapHandoverItem(ContractHandoverItemEntity item) {
        FileMetadataEntity evidenceFile = item.getEvidenceFile();
        return ContractHandoverDetailsResponse.HandoverItemDetails.builder()
                .id(item.getId())
                .assetName(item.getAssetName())
                .quantity(item.getQuantity())
                .conditionStatus(item.getConditionStatus())
                .note(item.getNote())
                .evidenceFileId(evidenceFile != null ? evidenceFile.getId() : null)
                .evidenceFileUrl(evidenceFile == null ? null : "/api/v1/files/" + evidenceFile.getId())
                .compensationAmount(item.getCompensationAmount())
                .compensationInvoiceId(item.getCompensationInvoice() == null ? null : item.getCompensationInvoice().getId())
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
