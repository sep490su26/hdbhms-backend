package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaMeterReadingRepository extends JpaRepository<MeterReadingEntity, Long> {
    @Query("""
            SELECT reading FROM MeterReadingEntity reading
            JOIN FETCH reading.meter meter
            WHERE reading.room.id = :roomId
              AND reading.status <> com.sep490.hdbhms.occupancy.domain.value_objects.ReadingStatus.VOIDED
            ORDER BY reading.readingPeriod DESC, reading.readingDate DESC, reading.createdAt DESC, reading.id DESC
            """)
    List<MeterReadingEntity> findActiveByRoomIdLatestFirst(@Param("roomId") Long roomId);
}
