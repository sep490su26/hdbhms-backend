package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.domain.policy.ReadingWindow;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterType;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingBatchEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingBatchRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.BatchMeterReadingStatusResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.MeterReadingBatchHistoryResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.UtilityDashboardResponse;
import com.sep490.hdbhms.occupancy.domain.value_objects.BatchStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetBatchMeterReadingsService {

    JpaRoomRepository roomRepository;
    JpaMeterReadingRepository meterReadingRepository;
    JpaMeterReadingBatchRepository batchRepository;

    @Transactional(readOnly = true)
    public BatchMeterReadingStatusResponse getBatchStatus(String period, Long propertyId) {
        String resolvedPeriod = MeterReadingPeriod.normalize(period);
        YearMonth ym = MeterReadingPeriod.parse(resolvedPeriod);
        LocalDate startOfPeriod = ym.atDay(1);

        // 1. Fetch all rooms
        List<RoomEntity> rooms;
        if (propertyId != null) {
            rooms = roomRepository.findAllByProperty_IdAndDeletedAtIsNullOrderBySortOrderAscRoomCodeAsc(propertyId);
        } else {
            rooms = roomRepository.findAll();
        }

        // 2. Fetch current readings for the period
        List<MeterReadingEntity> currentReadings = meterReadingRepository
                .findByPeriodAndOptionalProperty(resolvedPeriod, propertyId);

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
            if (currElec != null || currWater != null) {
                // Determine status based on DB. Default to synced if exists.
                // Could be 'error' if negative usage, but let's map standard status.
                status = "synced";
            }

            int photosCount = 0;
            if (currElec != null && currElec.getPhotoFile() != null) photosCount++;
            if (currWater != null && currWater.getPhotoFile() != null) photosCount++;

            return BatchMeterReadingStatusResponse.RoomBatchStatus.builder()
                    .roomId(room.getId())
                    .roomCode(room.getRoomCode())
                    .roomName(room.getName())
                    .electricityPrevious(currElec != null ? currElec.getPreviousValue() : (prevElec != null ? prevElec.getCurrentValue() : null))
                    .electricityCurrent(currElec != null ? currElec.getCurrentValue() : null)
                    .waterPrevious(currWater != null ? currWater.getPreviousValue() : (prevWater != null ? prevWater.getCurrentValue() : null))
                    .waterCurrent(currWater != null ? currWater.getCurrentValue() : null)
                    .status(status)
                    .syncTime(currElec != null ? currElec.getCreatedAt() : (currWater != null ? currWater.getCreatedAt() : null))
                    .photosCount(photosCount)
                    .build();
        }).collect(Collectors.toList());

        MeterReadingBatchEntity batch = null;
        if (propertyId != null) {
            batch = batchRepository.findByProperty_IdAndReadingPeriod(propertyId, resolvedPeriod).orElse(null);
        }

        return BatchMeterReadingStatusResponse.builder()
                .batchId(batch != null ? batch.getId() : null)
                .batchStatus(batch != null ? batch.getStatus().name() : null)
                .rooms(statusList)
                .build();
    }

    @Transactional(readOnly = true)
    public MeterReadingBatchHistoryResponse getBatchHistory(Long propertyId) {
        List<com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingBatchEntity> batches;
        if (propertyId != null) {
            batches = batchRepository.findByProperty_IdOrderByReadingPeriodDesc(propertyId);
        } else {
            batches = batchRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "readingPeriod"));
        }

        List<MeterReadingBatchHistoryResponse.BatchHistoryItem> history = batches.stream().map(b -> {
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
        }).sorted(Comparator.comparing(
                (MeterReadingBatchHistoryResponse.BatchHistoryItem item) -> MeterReadingPeriod.parse(item.getPeriod())
        ).reversed()).collect(Collectors.toList());

        return MeterReadingBatchHistoryResponse.builder()
                .history(history)
                .build();
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
                    .canCreateCurrentPeriod(false)
                    .currentPeriod(
                            UtilityDashboardResponse.CurrentPeriodInfo.builder()
                            .id(batch.getId())
                            .readingPeriod(MeterReadingPeriod.normalize(batch.getReadingPeriod()))
                            .status(batch.getStatus())
                            .build()
                    )
                    .build();
        }

        boolean canCreate = ReadingWindow.isOpen(today);
        LocalDate nextAvailableDate = canCreate ? null : ReadingWindow.calculateNextOpenDate(today);

        return UtilityDashboardResponse.builder()
                .canCreateCurrentPeriod(canCreate)
                .nextAvailableDate(nextAvailableDate)
                .currentPeriod(null)
                .build();
    }
}
