package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.billingandpayment.application.service.UtilityBillingRunService;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceReason;
import com.sep490.hdbhms.file.application.port.out.FileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.occupancy.application.port.in.command.SubmitBatchMeterReadingsCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.SubmitSingleMeterReadingCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.SubmitMeterReadingUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.*;
import com.sep490.hdbhms.occupancy.domain.model.*;
import com.sep490.hdbhms.occupancy.domain.value_objects.BatchStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.AnomalySeverity;
import com.sep490.hdbhms.occupancy.domain.value_objects.AnomalyType;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterReadingReviewStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterType;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingSource;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingAnomalyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingAnomalyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingBatchRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubmitMeterReadingService implements SubmitMeterReadingUseCase {
    RoomRepository roomRepository;
    PropertyRepository propertyRepository;
    LeaseContractRepository leaseContractRepository;
    MeterRepository meterRepository;
    MeterReadingRepository meterReadingRepository;
    MeterReadingBatchRepository meterReadingBatchRepository;
    FileMetadataRepository fileMetadataRepository;
    UserRepository userRepository;
    JpaMeterReadingAnomalyRepository meterReadingAnomalyRepository;
    JpaMeterReadingRepository jpaMeterReadingRepository;
    JpaMeterReadingBatchRepository jpaMeterReadingBatchRepository;
    UtilityBillingRunService utilityBillingRunService;
    JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void submitSingleReading(SubmitSingleMeterReadingCommand command) {
        String readingPeriod = MeterReadingPeriod.normalize(command.readingPeriod());
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        Room room = roomRepository.findById(command.roomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        assertRoomRequiresMeterReading(room.getPropertyId(), room.getId(), readingPeriod);

        // Submit electricity reading
        submitMeterValue(room, MeterType.ELECTRICITY, command.electricityValue(), 
                readingPeriod, command.readingDate(), command.electricityPhotoId(),
                null, currentUser);

        // Submit water reading
        submitMeterValue(room, MeterType.WATER, command.waterValue(), 
                readingPeriod, command.readingDate(), command.waterPhotoId(),
                null, currentUser);
    }

    @Override
    @Transactional
    public void submitBatchReadings(SubmitBatchMeterReadingsCommand command) {
        String readingPeriod = MeterReadingPeriod.normalize(command.readingPeriod());
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new AppException(ApiErrorCode.PROPERTY_NOT_FOUND));
        int totalRooms = requireMeterReadingRooms(property.getId(), readingPeriod);

        // Create the batch record
        MeterReadingBatch batch = MeterReadingBatch.builder()
                .propertyId(property.getId())
                .readingPeriod(readingPeriod)
                .totalRooms(totalRooms)
                .status(BatchStatus.CONFIRMED)
                .createdById(currentUser.getId())
                .confirmedById(currentUser.getId())
                .confirmedAt(java.time.LocalDateTime.now())
                .build();
        batch = meterReadingBatchRepository.save(batch);

        for (SubmitBatchMeterReadingsCommand.RoomReading input : command.readings()) {
            Room room = roomRepository.findById(input.roomId())
                    .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));

            // Validate that room belongs to the property
            if (!room.getPropertyId().equals(property.getId())) {
                throw new AppException(ApiErrorCode.VISIT_002); // Invalid room property
            }
            assertRoomRequiresMeterReading(property.getId(), room.getId(), readingPeriod);

            submitMeterValue(room, MeterType.ELECTRICITY, input.electricityValue(), 
                    readingPeriod, command.readingDate(), input.electricityPhotoId(),
                    batch, currentUser);

            submitMeterValue(room, MeterType.WATER, input.waterValue(), 
                    readingPeriod, command.readingDate(), input.waterPhotoId(),
                    batch, currentUser);
        }
        createMonthlyUtilityBillingBatch(batch, currentUser.getId());
    }

    private void submitMeterValue(Room room, MeterType meterType, BigDecimal newValue,
                                  String period, LocalDate readingDate, Long photoId,
                                  MeterReadingBatch batch, User currentUser) {
        requireMeterValue(newValue);

        // Find active meter for the room
        Meter activeMeter = meterRepository.findFirstByRoomIdAndMeterTypeAndStatus(
                room.getId(), meterType, MeterStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ApiErrorCode.METER_NOT_FOUND));

        // Get previous value (latest reading currentValue)
        var latestReadingOpt = meterReadingRepository.findFirstByRoomIdAndMeterTypeOrderByReadingDateDesc(
                room.getId(), meterType);
        MeterReading previousReading = latestReadingOpt.orElse(null);
        BigDecimal prevValue = previousReading == null ? BigDecimal.ZERO : previousReading.getCurrentValue();
        BigDecimal previousCycleUsage = usageOf(previousReading);

        // Determine next revision number for the period
        int nextRevision = 1;
        var existingPeriodReadingOpt = meterReadingRepository.findFirstByMeterIdAndReadingPeriodOrderByRevisionNoDesc(
                activeMeter.getId(), period);
        if (existingPeriodReadingOpt.isPresent()) {
            nextRevision = existingPeriodReadingOpt.get().getRevisionNo() + 1;
        }

        MeterReading reading = MeterReading.builder()
                .meterId(activeMeter.getId())
                .roomId(room.getId())
                .readingPeriod(period)
                .revisionNo(nextRevision)
                .previousValue(prevValue)
                .currentValue(newValue)
                .readingDate(readingDate)
                .source(ReadingSource.MANUAL)
                .status(ReadingStatus.CONFIRMED)
                .batchId(batch != null ? batch.getId() : null)
                .createdById(currentUser.getId())
                .photoFileId(photoId)
                .build();

        saveReadingWithReview(reading, meterType, previousCycleUsage);
    }

    @Override
    @Transactional
    public Long startBatch(String period, Long propertyId) {
        String readingPeriod = MeterReadingPeriod.normalize(period);
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new AppException(ApiErrorCode.PROPERTY_NOT_FOUND));
        int totalRooms = requireMeterReadingRooms(property.getId(), readingPeriod);

        Optional<MeterReadingBatch> existingBatch = selectPreferredBatch(
                meterReadingBatchRepository.findByPropertyIdAndReadingPeriodOrderByIdDesc(propertyId, readingPeriod)
        );
        if (existingBatch.isPresent()) return existingBatch.get().getId();

        MeterReadingBatch batch = MeterReadingBatch.builder()
                .propertyId(property.getId())
                .readingPeriod(readingPeriod)
                .totalRooms(totalRooms)
                .status(BatchStatus.DRAFT)
                .createdById(currentUser.getId())
                .build();
        batch = meterReadingBatchRepository.save(batch);
        return batch.getId();
    }

    static Optional<MeterReadingBatch> selectPreferredBatch(List<MeterReadingBatch> batches) {
        return batches.stream()
                .max(Comparator
                        .comparingInt(SubmitMeterReadingService::statusRank)
                        .thenComparing(batch -> batch.getId() == null ? 0L : batch.getId()));
    }

    private static int statusRank(MeterReadingBatch batch) {
        String status = batch.getStatus() == null ? "" : batch.getStatus().name();
        return switch (status) {
            case "DRAFT" -> 4;
            case "PREVIEWED" -> 3;
            case "CONFIRMED" -> 2;
            case "CANCELLED" -> 1;
            default -> 0;
        };
    }

    @Override
    @Transactional
    public void saveProgressiveRoomReading(Long batchId, Long roomId, BigDecimal electricityValue, BigDecimal waterValue, Long elecPhotoId, Long waterPhotoId) {
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));

        MeterReadingBatch batch = meterReadingBatchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(ApiErrorCode.METER_READING_BATCH_NOT_FOUND));

        if (batch.getStatus() == BatchStatus.CANCELLED) {
            throw new AppException(ApiErrorCode.METER_READING_BATCH_CANCELLED);
        }
        if (batch.getStatus() == BatchStatus.CONFIRMED) {
            throw new AppException(ApiErrorCode.METER_READING_BATCH_CONFIRMED);
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));

        if (!room.getPropertyId().equals(batch.getPropertyId())) {
            throw new AppException(ApiErrorCode.VISIT_002);
        }
        assertRoomRequiresMeterReading(batch.getPropertyId(), room.getId(), batch.getReadingPeriod());

        // Electricity
        saveOrUpdateMeterValue(room, MeterType.ELECTRICITY, electricityValue, batch, elecPhotoId, currentUser);
        
        // Water
        saveOrUpdateMeterValue(room, MeterType.WATER, waterValue, batch, waterPhotoId, currentUser);
        refreshBatchProgress(batch.getId(), batch.getPropertyId(), batch.getReadingPeriod());
    }

    private void saveOrUpdateMeterValue(Room room, MeterType meterType, BigDecimal newValue, MeterReadingBatch batch, Long photoId, User currentUser) {
        if (newValue == null) return;
        requireMeterValue(newValue);

        Meter activeMeter = meterRepository.findFirstByRoomIdAndMeterTypeAndStatus(
                room.getId(), meterType, MeterStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ApiErrorCode.METER_NOT_FOUND));

        var existingReadingOpt = meterReadingRepository.findByMeterIdAndBatchId(activeMeter.getId(), batch.getId());
        if (existingReadingOpt.isPresent()) {
            MeterReading existing = existingReadingOpt.get();
            existing.setBatchId(batch.getId());
            existing.setCurrentValue(newValue);
            existing.setPhotoFileId(photoId);
            saveReadingWithReview(existing, meterType, findPreviousCycleUsage(activeMeter.getId(), existing.getReadingDate()));
            return;
        }

        var existingPeriodReadingOpt = meterReadingRepository.findFirstByMeterIdAndReadingPeriodOrderByRevisionNoDesc(
                activeMeter.getId(),
                batch.getReadingPeriod()
        );
        if (existingPeriodReadingOpt.isPresent()
                && existingPeriodReadingOpt.get().getStatus() != ReadingStatus.VOIDED) {
            MeterReading existing = existingPeriodReadingOpt.get();
            existing.setBatchId(batch.getId());
            existing.setCurrentValue(newValue);
            existing.setPhotoFileId(photoId);
            saveReadingWithReview(existing, meterType, findPreviousCycleUsage(activeMeter.getId(), existing.getReadingDate()));
            return;
        }

        var latestReadingOpt = meterReadingRepository.findFirstByRoomIdAndMeterTypeOrderByReadingDateDesc(
                room.getId(), meterType);
        
        MeterReading previousReading = latestReadingOpt.orElse(null);
        BigDecimal prevValue = previousReading == null ? BigDecimal.ZERO : previousReading.getCurrentValue();
        int nextRevision = existingPeriodReadingOpt
                .map(MeterReading::getRevisionNo)
                .map(revision -> revision + 1)
                .orElse(1);
        MeterReading reading = MeterReading.builder()
                .meterId(activeMeter.getId())
                .roomId(room.getId())
                .readingPeriod(batch.getReadingPeriod())
                .revisionNo(nextRevision)
                .previousValue(prevValue)
                .currentValue(newValue)
                .readingDate(LocalDate.now())
                .source(ReadingSource.MANUAL)
                .status(ReadingStatus.CONFIRMED)
                .batchId(batch.getId())
                .createdById(currentUser.getId())
                .photoFileId(photoId)
                .build();
        saveReadingWithReview(reading, meterType, usageOf(previousReading));
    }

    private MeterReading saveReadingWithReview(
            MeterReading reading,
            MeterType meterType,
            BigDecimal previousCycleUsage
    ) {
        List<DetectedAnomaly> anomalies = detectAnomalies(
                meterType,
                reading.getPreviousValue(),
                reading.getCurrentValue(),
                previousCycleUsage
        );
        reading.setReviewStatus(anomalies.isEmpty()
                ? MeterReadingReviewStatus.NONE
                : MeterReadingReviewStatus.PENDING);
        reading.setReviewCount(anomalies.size());

        MeterReading saved = meterReadingRepository.save(reading);
        replaceAnomalies(saved, anomalies);
        refreshBatchAnomalyCount(saved.getBatchId());
        return saved;
    }

    private void replaceAnomalies(MeterReading reading, List<DetectedAnomaly> anomalies) {
        if (reading.getId() == null) {
            return;
        }
        meterReadingAnomalyRepository.deleteByMeterReading_IdAndResolvedAtIsNull(reading.getId());
        if (anomalies.isEmpty()) {
            return;
        }

        var readingRef = jpaMeterReadingRepository.getReferenceById(reading.getId());
        var batchRef = reading.getBatchId() == null
                ? null
                : jpaMeterReadingBatchRepository.getReferenceById(reading.getBatchId());
        meterReadingAnomalyRepository.saveAll(anomalies.stream()
                .map(anomaly -> MeterReadingAnomalyEntity.builder()
                        .batch(batchRef)
                        .meterReading(readingRef)
                        .anomalyType(anomaly.type())
                        .severity(anomaly.severity())
                        .message(anomaly.message())
                        .build())
                .toList());
    }

    private void refreshBatchAnomalyCount(Long batchId) {
        if (batchId == null) {
            return;
        }
        jpaMeterReadingBatchRepository.findById(batchId).ifPresent(batch -> {
            long count = meterReadingAnomalyRepository.countByBatch_IdAndResolvedAtIsNull(batchId);
            batch.setAnomalyCount(count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count);
            jpaMeterReadingBatchRepository.save(batch);
        });
    }

    private void refreshBatchProgress(Long batchId, Long propertyId, String readingPeriod) {
        if (batchId == null) {
            return;
        }

        int totalRooms = countMeterReadingRooms(propertyId, readingPeriod);
        int completedRooms = countCompletedRooms(batchId, propertyId, readingPeriod);
        jpaMeterReadingBatchRepository.findById(batchId).ifPresent(batch -> {
            batch.setTotalRooms(totalRooms);
            batch.setCompletedRooms(completedRooms);
            jpaMeterReadingBatchRepository.save(batch);
        });
    }

    private List<DetectedAnomaly> detectAnomalies(
            MeterType meterType,
            BigDecimal previousValue,
            BigDecimal currentValue,
            BigDecimal previousCycleUsage
    ) {
        List<DetectedAnomaly> anomalies = new ArrayList<>();
        BigDecimal usage = safe(currentValue).subtract(safe(previousValue));
        if (usage.compareTo(BigDecimal.ZERO) < 0) {
            anomalies.add(new DetectedAnomaly(
                    AnomalyType.NEGATIVE_USAGE,
                    AnomalySeverity.HIGH,
                    "Chỉ số " + utilityLabel(meterType) + " mới nhỏ hơn chỉ số trước đó."
            ));
            return anomalies;
        }

        if (isHighUsage(meterType, usage, previousCycleUsage)) {
            anomalies.add(new DetectedAnomaly(
                    AnomalyType.HIGH_USAGE,
                    AnomalySeverity.MEDIUM,
                    highUsageMessage(meterType, usage, previousCycleUsage)
            ));
        }
        return anomalies;
    }

    private boolean isHighUsage(MeterType meterType, BigDecimal usage, BigDecimal previousCycleUsage) {
        if (usage.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        // ponytail: fixed abnormal thresholds; move to property-level utility settings when the UI exposes per-building rules.
        if (previousCycleUsage != null && previousCycleUsage.compareTo(BigDecimal.ZERO) > 0) {
            return usage.compareTo(previousCycleUsage.multiply(BigDecimal.valueOf(3))) > 0
                    && usage.subtract(previousCycleUsage).compareTo(highUsageDelta(meterType)) >= 0;
        }
        return usage.compareTo(highUsageAbsoluteLimit(meterType)) > 0;
    }

    private String highUsageMessage(MeterType meterType, BigDecimal usage, BigDecimal previousCycleUsage) {
        if (previousCycleUsage != null && previousCycleUsage.compareTo(BigDecimal.ZERO) > 0) {
            return "Mức tiêu thụ " + utilityLabel(meterType)
                    + " là " + usage.stripTrailingZeros().toPlainString()
                    + ", cao hơn kỳ trước "
                    + previousCycleUsage.stripTrailingZeros().toPlainString()
                    + " quá 3 lần.";
        }
        return "Mức tiêu thụ " + utilityLabel(meterType)
                + " là " + usage.stripTrailingZeros().toPlainString()
                + ", vượt ngưỡng cần kiểm tra.";
    }

    private BigDecimal findPreviousCycleUsage(Long meterId, LocalDate readingDate) {
        if (readingDate == null) {
            return null;
        }
        return meterReadingRepository.findByMeterIdAndReadingDateBeforeOrderByReadingDateDesc(meterId, readingDate)
                .stream()
                .findFirst()
                .map(this::usageOf)
                .orElse(null);
    }

    private BigDecimal usageOf(MeterReading reading) {
        if (reading == null) {
            return null;
        }
        if (reading.getUsageAmount() != null) {
            return reading.getUsageAmount();
        }
        return safe(reading.getCurrentValue()).subtract(safe(reading.getPreviousValue()));
    }

    private BigDecimal highUsageDelta(MeterType meterType) {
        return meterType == MeterType.WATER ? BigDecimal.valueOf(10) : BigDecimal.valueOf(50);
    }

    private BigDecimal highUsageAbsoluteLimit(MeterType meterType) {
        return meterType == MeterType.WATER ? BigDecimal.valueOf(50) : BigDecimal.valueOf(500);
    }

    private String utilityLabel(MeterType meterType) {
        return meterType == MeterType.WATER ? "nước" : "điện";
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void requireMeterValue(BigDecimal newValue) {
        if (newValue == null || newValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ApiErrorCode.INVALID_METER_READING_VALUE);
        }
    }

    private record DetectedAnomaly(
            AnomalyType type,
            AnomalySeverity severity,
            String message
    ) {
    }

    @Override
    @Transactional
    public void confirmBatch(Long batchId) {
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));

        MeterReadingBatch batch = meterReadingBatchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(ApiErrorCode.METER_READING_BATCH_NOT_FOUND));
        requireMeterReadingRooms(batch.getPropertyId(), batch.getReadingPeriod());

        if (batch.getStatus() == BatchStatus.CONFIRMED) {
            return;
        }

        // Validate no pending rooms...
        // Simplified: just update status
        batch.setStatus(BatchStatus.CONFIRMED);
        batch.setConfirmedById(currentUser.getId());
        batch.setConfirmedAt(java.time.LocalDateTime.now());

        meterReadingBatchRepository.save(batch);
        refreshBatchProgress(batch.getId(), batch.getPropertyId(), batch.getReadingPeriod());
        createMonthlyUtilityBillingBatch(batch, currentUser.getId());
    }

    private void createMonthlyUtilityBillingBatch(MeterReadingBatch batch, Long currentUserId) {
        requireCompletedReadings(batch);
        String billingPeriod = MeterReadingPeriod.parse(batch.getReadingPeriod()).toString();
        utilityBillingRunService.createPreview(
                batch.getPropertyId(),
                billingPeriod,
                InvoiceReason.MONTHLY.name(),
                currentUserId
        );
    }

    private void requireCompletedReadings(MeterReadingBatch batch) {
        int totalRooms = requireMeterReadingRooms(batch.getPropertyId(), batch.getReadingPeriod());
        int completedRooms = countCompletedRooms(batch.getId(), batch.getPropertyId(), batch.getReadingPeriod());
        if (completedRooms < totalRooms) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot confirm meter reading batch before all eligible rooms have electricity and water readings."
            );
        }
    }

    private int countCompletedRooms(Long batchId, Long propertyId, String readingPeriod) {
        YearMonth period = MeterReadingPeriod.parse(readingPeriod);
        Integer completedRooms = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1)
                        FROM (
                            SELECT mr.room_id
                            FROM meter_readings mr
                            JOIN meter_reading_batches mb
                              ON mb.meter_reading_batch_id = mr.batch_id
                            JOIN meters m
                              ON m.meter_id = mr.meter_id
                            JOIN (
                                SELECT DISTINCT lc.room_id
                                FROM lease_contracts lc
                                JOIN rooms r
                                  ON r.room_id = lc.room_id
                                LEFT JOIN contract_liquidations cl
                                  ON cl.contract_id = lc.lease_contract_id
                                 AND cl.status = 'CONFIRMED'
                                WHERE lc.deleted_at IS NULL
                                  AND r.deleted_at IS NULL
                                  AND lc.status IN (
                                      'ACTIVE',
                                      'EXPIRING_SOON',
                                      'TERMINATION_PENDING',
                                      'EXPIRED',
                                      'LIQUIDATED',
                                      'RENEWED'
                                  )
                                  AND (? IS NULL OR r.property_id = ?)
                                  AND COALESCE(lc.rent_start_date, lc.start_date) <= ?
                                  AND (
                                      COALESCE(cl.liquidation_date, lc.end_date) IS NULL
                                      OR COALESCE(cl.liquidation_date, lc.end_date) >= ?
                                  )
                            ) eligible_rooms
                              ON eligible_rooms.room_id = mr.room_id
                            WHERE mr.batch_id = ?
                              AND mr.status <> 'VOIDED'
                              AND (
                                  mb.status <> 'CONFIRMED'
                                  OR mb.confirmed_at IS NULL
                                  OR mr.created_at <= mb.confirmed_at
                              )
                              AND m.meter_type IN ('ELECTRICITY', 'WATER')
                            GROUP BY mr.room_id
                            HAVING COUNT(DISTINCT m.meter_type) = 2
                        ) completed_rooms
                        """,
                Integer.class,
                propertyId,
                propertyId,
                period.atEndOfMonth(),
                period.atDay(1),
                batchId
        );
        return completedRooms == null ? 0 : completedRooms;
    }

    private int countMeterReadingRooms(Long propertyId, String readingPeriod) {
        YearMonth period = MeterReadingPeriod.parse(readingPeriod);
        return Math.toIntExact(leaseContractRepository.countMeterReadingRoomsByPeriod(
                propertyId,
                MeterReadingContractEligibility.STATUSES,
                period.atDay(1),
                period.atEndOfMonth()
        ));
    }

    private int requireMeterReadingRooms(Long propertyId, String readingPeriod) {
        int totalRooms = countMeterReadingRooms(propertyId, readingPeriod);
        if (totalRooms == 0) {
            throw new AppException(ApiErrorCode.METER_READING_NO_ELIGIBLE_ROOMS);
        }
        return totalRooms;
    }

    private void assertRoomRequiresMeterReading(Long propertyId, Long roomId, String readingPeriod) {
        YearMonth period = MeterReadingPeriod.parse(readingPeriod);
        boolean required = leaseContractRepository.roomRequiresMeterReadingForPeriod(
                propertyId,
                roomId,
                MeterReadingContractEligibility.STATUSES,
                period.atDay(1),
                period.atEndOfMonth()
        );
        if (!required) {
            throw new AppException(ApiErrorCode.METER_READING_ROOM_NOT_ELIGIBLE);
        }
    }
}
