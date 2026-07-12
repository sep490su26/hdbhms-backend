package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.MeterReadingListResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetMeterReadingsService {

    JpaMeterReadingRepository meterReadingRepository;

    /**
     * Returns readings grouped by room.
     *
     * @param period     yyyy-MM or MM/yyyy; if null, defaults to current month
     * @param propertyId optional property filter
     */
    @Transactional(readOnly = true)
    public MeterReadingListResponse getReadings(String period, Long propertyId) {
        String resolvedPeriod = MeterReadingPeriod.normalize(period);

        List<MeterReadingEntity> readings = meterReadingRepository
                .findByPeriodAndOptionalProperty(resolvedPeriod, propertyId);

        return MeterReadingListResponse.builder()
                .rooms(groupByRoom(readings))
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────

    private List<MeterReadingListResponse.RoomReadingGroup> groupByRoom(List<MeterReadingEntity> readings) {
        // LinkedHashMap preserves insertion order (already sorted by DB query)
        Map<Long, MeterReadingListResponse.RoomReadingGroup.RoomReadingGroupBuilder> builders = new LinkedHashMap<>();

        for (MeterReadingEntity r : readings) {
            var room = r.getRoom();
            var property = room.getProperty();

            builders.computeIfAbsent(room.getId(), id ->
                    MeterReadingListResponse.RoomReadingGroup.builder()
                            .roomId(room.getId())
                            .roomCode(room.getRoomCode())
                            .roomName(room.getName())
                            .propertyId(property.getId())
                            .propertyName(property.getName())
                            .readings(new ArrayList<>())
            );

            builders.get(room.getId()).build().getReadings().add(toEntry(r));
        }

        // Re-build so readings list is attached properly
        Map<Long, List<MeterReadingListResponse.MeterReadingEntry>> entriesMap = new LinkedHashMap<>();
        for (MeterReadingEntity r : readings) {
            entriesMap.computeIfAbsent(r.getRoom().getId(), id -> new ArrayList<>()).add(toEntry(r));
        }

        List<MeterReadingListResponse.RoomReadingGroup> result = new ArrayList<>();
        for (MeterReadingEntity r : readings) {
            var room = r.getRoom();
            if (!result.stream().anyMatch(g -> g.getRoomId().equals(room.getId()))) {
                var property = room.getProperty();
                result.add(MeterReadingListResponse.RoomReadingGroup.builder()
                        .roomId(room.getId())
                        .roomCode(room.getRoomCode())
                        .roomName(room.getName())
                        .propertyId(property.getId())
                        .propertyName(property.getName())
                        .readings(entriesMap.get(room.getId()))
                        .build());
            }
        }
        return result;
    }

    private MeterReadingListResponse.MeterReadingEntry toEntry(MeterReadingEntity e) {
        String createdByName = null;
        if (e.getCreatedBy() != null) {
            createdByName = e.getCreatedBy().getPhone();
        }
        return MeterReadingListResponse.MeterReadingEntry.builder()
                .id(e.getId())
                .meterType(e.getMeter().getMeterType())
                .readingPeriod(e.getReadingPeriod())
                .previousValue(e.getPreviousValue())
                .currentValue(e.getCurrentValue())
                .usageAmount(e.getUsageAmount())
                .readingDate(e.getReadingDate())
                .source(e.getSource())
                .status(e.getStatus())
                .photoFileId(e.getPhotoFile() != null ? e.getPhotoFile().getId() : null)
                .createdByName(createdByName)
                .createdAt(e.getCreatedAt())
                .build();
    }
}
