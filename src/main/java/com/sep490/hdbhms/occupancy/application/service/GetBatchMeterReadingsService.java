package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.domain.policy.ReadingWindow;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterType;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingAnomalyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingBatchEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingAnomalyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingBatchRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.BatchMeterReadingStatusResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.MeterReadingBatchHistoryResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.UtilityDashboardResponse;
import com.sep490.hdbhms.occupancy.domain.value_objects.BatchStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.UtilityType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetBatchMeterReadingsService {
    JpaLeaseContractRepository leaseContractRepository;
    JpaMeterReadingRepository meterReadingRepository;
    JpaMeterReadingAnomalyRepository anomalyRepository;
    JpaMeterReadingBatchRepository batchRepository;
    JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public BatchMeterReadingStatusResponse getBatchStatus(String period, Long propertyId) {
        String resolvedPeriod = MeterReadingPeriod.normalize(period);
        YearMonth ym = MeterReadingPeriod.parse(resolvedPeriod);
        LocalDate startOfPeriod = ym.atDay(1);
        LocalDate endOfPeriod = ym.atEndOfMonth();
        LocalDate tariffDate = LocalDate.now();

        List<RoomEntity> rooms = leaseContractRepository.findMeterReadingRoomsByPeriod(
                propertyId,
                MeterReadingContractEligibility.STATUSES,
                startOfPeriod,
                endOfPeriod
        );

        // 2. Fetch current readings for the period
        List<MeterReadingEntity> currentReadings = meterReadingRepository
                .findByPeriodAndOptionalProperty(resolvedPeriod, propertyId);
        Map<Long, List<MeterReadingAnomalyEntity>> anomaliesByReading = currentReadings.isEmpty()
                ? Map.of()
                : anomalyRepository.findByMeterReading_IdInAndResolvedAtIsNullOrderByIdAsc(
                                currentReadings.stream().map(MeterReadingEntity::getId).toList()
                        )
                        .stream()
                        .collect(Collectors.groupingBy(anomaly -> anomaly.getMeterReading().getId()));

        // 3. Fetch latest readings BEFORE the start of the period
        List<MeterReadingEntity> previousReadings = meterReadingRepository
                .findLatestBeforeDateByProperty(propertyId, startOfPeriod);

        // Group readings by room
        Map<Long, List<MeterReadingEntity>> currentByRoom = currentReadings.stream()
                .collect(Collectors.groupingBy(r -> r.getRoom().getId()));

        Map<Long, List<MeterReadingEntity>> previousByRoom = previousReadings.stream()
                .collect(Collectors.groupingBy(r -> r.getRoom().getId()));

        List<BatchMeterReadingStatusResponse.RoomBatchStatus> statusList = rooms.stream().map(room -> {
            List<MeterReadingEntity> curr = currentByRoom.getOrDefault(room.getId(), List.of());
            List<MeterReadingEntity> prev = previousByRoom.getOrDefault(room.getId(), List.of());

            MeterReadingEntity currElec = curr.stream().filter(r -> r.getMeter().getMeterType() == MeterType.ELECTRICITY).findFirst().orElse(null);
            MeterReadingEntity currWater = curr.stream().filter(r -> r.getMeter().getMeterType() == MeterType.WATER).findFirst().orElse(null);

            MeterReadingEntity prevElec = prev.stream().filter(r -> r.getMeter().getMeterType() == MeterType.ELECTRICITY).findFirst().orElse(null);
            MeterReadingEntity prevWater = prev.stream().filter(r -> r.getMeter().getMeterType() == MeterType.WATER).findFirst().orElse(null);

            String status = "pending";
            if (currElec != null && currWater != null
                    && currElec.getPhotoFile() != null
                    && currWater.getPhotoFile() != null) {
                status = "synced";
            }

            int photosCount = 0;
            if (currElec != null && currElec.getPhotoFile() != null) photosCount++;
            if (currWater != null && currWater.getPhotoFile() != null) photosCount++;

            List<BatchMeterReadingStatusResponse.ReadingWarning> warnings = buildWarnings(
                    currElec,
                    currWater,
                    anomaliesByReading
            );
            if (!warnings.isEmpty() && currElec != null && currWater != null) {
                status = "warning";
            }

            return BatchMeterReadingStatusResponse.RoomBatchStatus.builder()
                    .roomId(room.getId())
                    .roomCode(room.getRoomCode())
                    .roomName(room.getName())
                    .electricityPrevious(currElec != null ? currElec.getPreviousValue() : (prevElec != null ? prevElec.getCurrentValue() : null))
                    .electricityCurrent(currElec != null ? currElec.getCurrentValue() : null)
                    .electricityPhotoId(currElec != null && currElec.getPhotoFile() != null ? currElec.getPhotoFile().getId() : null)
                    .waterPrevious(currWater != null ? currWater.getPreviousValue() : (prevWater != null ? prevWater.getCurrentValue() : null))
                    .waterCurrent(currWater != null ? currWater.getCurrentValue() : null)
                    .waterPhotoId(currWater != null && currWater.getPhotoFile() != null ? currWater.getPhotoFile().getId() : null)
                    .status(status)
                    .syncTime(currElec != null ? currElec.getCreatedAt() : (currWater != null ? currWater.getCreatedAt() : null))
                    .photosCount(photosCount)
                    .warnings(warnings)
                    .build();
        }).collect(Collectors.toList());

        MeterReadingBatchEntity batch = null;
        if (propertyId != null) {
            batch = selectPreferredBatch(
                    batchRepository.findAllByProperty_IdAndReadingPeriodOrderByIdDesc(propertyId, resolvedPeriod)
            ).orElse(null);
        }

        return BatchMeterReadingStatusResponse.builder()
                .propertyId(propertyId)
                .propertyName(readPropertyName(propertyId))
                .batchId(batch != null ? batch.getId() : null)
                .batchStatus(batch != null ? batch.getStatus().name() : null)
                .electricityTariff(readUtilityTariff(propertyId, UtilityType.ELECTRICITY, tariffDate))
                .waterTariff(readUtilityTariff(propertyId, UtilityType.WATER, tariffDate))
                .rooms(statusList)
                .build();
    }

    private String readPropertyName(Long propertyId) {
        if (propertyId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                        SELECT name
                        FROM properties
                        WHERE property_id = ?
                          AND deleted_at IS NULL
                        LIMIT 1
                        """,
                (rs, rowNum) -> rs.getString("name"),
                propertyId
        ).stream().findFirst().orElse(null);
    }

    private List<BatchMeterReadingStatusResponse.ReadingWarning> buildWarnings(
            MeterReadingEntity electricity,
            MeterReadingEntity water,
            Map<Long, List<MeterReadingAnomalyEntity>> anomaliesByReading
    ) {
        List<BatchMeterReadingStatusResponse.ReadingWarning> warnings = new ArrayList<>();
        addReadingWarnings(warnings, "ELECTRICITY", electricity, anomaliesByReading);
        addReadingWarnings(warnings, "WATER", water, anomaliesByReading);
        return warnings;
    }

    private void addReadingWarnings(
            List<BatchMeterReadingStatusResponse.ReadingWarning> warnings,
            String meterType,
            MeterReadingEntity reading,
            Map<Long, List<MeterReadingAnomalyEntity>> anomaliesByReading
    ) {
        if (reading == null) {
            warnings.add(BatchMeterReadingStatusResponse.ReadingWarning.builder()
                    .meterType(meterType)
                    .type("MISSING_READING")
                    .severity("HIGH")
                    .message(("ELECTRICITY".equals(meterType) ? "Điện" : "Nước") + " chưa có chỉ số trong kỳ.")
                    .build());
            return;
        }

        anomaliesByReading.getOrDefault(reading.getId(), List.of()).forEach(anomaly ->
                warnings.add(BatchMeterReadingStatusResponse.ReadingWarning.builder()
                        .meterType(meterType)
                        .type(anomaly.getAnomalyType().name())
                        .severity(anomaly.getSeverity().name())
                        .message(anomaly.getMessage())
                        .build())
        );
    }

    private BatchMeterReadingStatusResponse.UtilityTariffSnapshot readUtilityTariff(
            Long propertyId,
            UtilityType utilityType,
            LocalDate readingDate
    ) {
        if (propertyId != null) {
            Optional<BatchMeterReadingStatusResponse.UtilityTariffSnapshot> tariff = jdbcTemplate.query("""
                            SELECT unit_price, free_allowance
                            FROM utility_tariffs
                            WHERE property_id = ?
                              AND utility_type = ?
                              AND effective_from <= ?
                              AND (effective_to IS NULL OR effective_to >= ?)
                            ORDER BY effective_from DESC, utility_tariff_id DESC
                            LIMIT 1
                            """,
                    (rs, rowNum) -> BatchMeterReadingStatusResponse.UtilityTariffSnapshot.builder()
                            .unitPrice(rs.getLong("unit_price"))
                            .freeAllowance(rs.getLong("free_allowance"))
                            .build(),
                    propertyId,
                    utilityType.name(),
                    readingDate,
                    readingDate
            ).stream().findFirst();
            if (tariff.isPresent()) return tariff.get();
        }

        return switch (utilityType) {
            case ELECTRICITY -> BatchMeterReadingStatusResponse.UtilityTariffSnapshot.builder()
                    .unitPrice(3500L)
                    .freeAllowance(0L)
                    .build();
            case WATER -> BatchMeterReadingStatusResponse.UtilityTariffSnapshot.builder()
                    .unitPrice(20000L)
                    .freeAllowance(6L)
                    .build();
            default -> BatchMeterReadingStatusResponse.UtilityTariffSnapshot.builder()
                    .unitPrice(0L)
                    .freeAllowance(0L)
                    .build();
        };
    }

    @Transactional(readOnly = true)
    public MeterReadingBatchHistoryResponse getBatchHistory(Long propertyId) {
        List<com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingBatchEntity> batches;
        if (propertyId != null) {
            batches = batchRepository.findByProperty_IdOrderByReadingPeriodDescIdDesc(propertyId);
        } else {
            batches = batchRepository.findAll(org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.desc("readingPeriod"),
                    org.springframework.data.domain.Sort.Order.desc("id")
            ));
        }

        List<MeterReadingBatchHistoryResponse.BatchHistoryItem> history = dedupeByPeriod(batches).stream().map(b -> {
            String period = MeterReadingPeriod.normalize(b.getReadingPeriod());
            YearMonth ym = MeterReadingPeriod.parse(period);
            return MeterReadingBatchHistoryResponse.BatchHistoryItem.builder()
                    .batchId(b.getId())
                    .period(period)
                    .isCurrent(b.getStatus() == BatchStatus.DRAFT)
                    .startDate(ym.atDay(1))
                    .endDate(ym.atEndOfMonth())
                    .status(b.getStatus())
                    .totalRooms(b.getTotalRooms())
                    .completedRooms(b.getCompletedRooms())
                    .anomalyCount(b.getAnomalyCount())
                    .build();
        }).collect(Collectors.toList());

        return MeterReadingBatchHistoryResponse.builder()
                .history(history)
                .build();
    }

    static List<MeterReadingBatchEntity> dedupeByPeriod(List<MeterReadingBatchEntity> batches) {
        Map<String, MeterReadingBatchEntity> byPeriod = new LinkedHashMap<>();
        for (MeterReadingBatchEntity batch : batches) {
            String period = MeterReadingPeriod.normalize(batch.getReadingPeriod());
            MeterReadingBatchEntity current = byPeriod.get(period);
            if (current == null || shouldReplaceBatch(current, batch)) {
                byPeriod.put(period, batch);
            }
        }
        return List.copyOf(byPeriod.values());
    }

    static Optional<MeterReadingBatchEntity> selectPreferredBatch(List<MeterReadingBatchEntity> batches) {
        return batches.stream()
                .max(Comparator
                        .comparingInt(GetBatchMeterReadingsService::statusRank)
                        .thenComparing(batch -> batch.getId() == null ? 0L : batch.getId()));
    }

    private static boolean shouldReplaceBatch(MeterReadingBatchEntity current, MeterReadingBatchEntity candidate) {
        return selectPreferredBatch(List.of(current, candidate)).orElse(current) == candidate;
    }

    private static int statusRank(MeterReadingBatchEntity batch) {
        String status = batch.getStatus() == null ? "" : batch.getStatus().name();
        return switch (status) {
            case "DRAFT" -> 4;
            case "PREVIEWED" -> 3;
            case "CONFIRMED" -> 2;
            case "CANCELLED" -> 1;
            default -> 0;
        };
    }

    @Transactional(readOnly = true)
    public UtilityDashboardResponse getDashboard(Long propertyId) {
        LocalDate today = LocalDate.now();

        Optional<MeterReadingBatchEntity> currentBatchOpt =
            propertyId != null 
                ? batchRepository.findFirstByProperty_IdAndStatusInOrderByCreatedAtDesc(
                        propertyId,
                        java.util.List.of(BatchStatus.DRAFT)
                )
                : java.util.Optional.empty();

        if (currentBatchOpt.isPresent()) {
            MeterReadingBatchEntity batch = currentBatchOpt.get();
            return UtilityDashboardResponse.builder()
                    .propertyId(propertyId)
                    .propertyName(readPropertyName(propertyId))
                    .canCreateCurrentPeriod(false)
                    .currentPeriod(
                            UtilityDashboardResponse.CurrentPeriodInfo.builder()
                            .id(batch.getId())
                            .readingPeriod(batch.getReadingPeriod())
                            .status(batch.getStatus())
                            .build()
                    )
                    .build();
        }

        boolean canCreate = ReadingWindow.isOpen(today);
        LocalDate nextAvailableDate = canCreate ? null : ReadingWindow.calculateNextOpenDate(today);

        return UtilityDashboardResponse.builder()
                .propertyId(propertyId)
                .propertyName(readPropertyName(propertyId))
                .canCreateCurrentPeriod(canCreate)
                .nextAvailableDate(nextAvailableDate)
                .currentPeriod(null)
                .build();
    }
}
