package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.occupancy.domain.valueObjects.MeterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaMeterReadingRepository extends JpaRepository<MeterReadingEntity, Long> {
    @Query("""
            SELECT reading FROM MeterReadingEntity reading
            WHERE reading.room.id = :roomId
              AND reading.meter.meterType = :meterType
              AND reading.status <> com.sep490.hdbhms.occupancy.domain.value_objects.ReadingStatus.VOIDED
            ORDER BY reading.readingDate DESC, reading.createdAt DESC, reading.id DESC
            """)
    Optional<MeterReadingEntity> findFirstByRoom_IdAndMeter_MeterTypeOrderByReadingDateDesc(
            @Param("roomId") Long roomId,
            @Param("meterType") MeterType meterType
    );
    Optional<MeterReadingEntity> findFirstByMeter_IdAndReadingPeriodOrderByRevisionNoDesc(Long meterId, String readingPeriod);
    Optional<MeterReadingEntity> findByMeter_IdAndBatchId(Long meterId, Long batchId);
    List<MeterReadingEntity> findByMeter_IdAndReadingDateBeforeOrderByReadingDateDesc(Long meterId, java.time.LocalDate readingDate);

    /**
     * Fetch all readings for a given period, optionally filtered by property.
     * Eagerly loads room → property so we can group them without N+1.
     */
    @Query("""
        SELECT r FROM MeterReadingEntity r
        JOIN FETCH r.room rm
        JOIN FETCH rm.property p
        LEFT JOIN FETCH r.createdBy u
        WHERE r.readingPeriod = :period
          AND (:propertyId IS NULL OR p.id = :propertyId)
        ORDER BY p.id, rm.roomCode, r.meter.meterType
    """)
    List<MeterReadingEntity> findByPeriodAndOptionalProperty(
            @Param("period") String period,
            @Param("propertyId") Long propertyId
    );

    /**
     * Fetch latest readings per meter across all rooms of a property (no period filter).
     */
    @Query("""
        SELECT r FROM MeterReadingEntity r
        JOIN FETCH r.room rm
        JOIN FETCH rm.property p
        LEFT JOIN FETCH r.createdBy u
        WHERE p.id = :propertyId
          AND r.readingDate = (
              SELECT MAX(r2.readingDate)
              FROM MeterReadingEntity r2
              WHERE r2.room.id = rm.id AND r2.meter.meterType = r.meter.meterType
          )
        ORDER BY rm.roomCode, r.meter.meterType
    """)
    List<MeterReadingEntity> findLatestPerRoomByProperty(@Param("propertyId") Long propertyId);
    @Query("""
            SELECT reading FROM MeterReadingEntity reading
            JOIN FETCH reading.meter meter
            WHERE reading.room.id = :roomId
              AND reading.status <> com.sep490.hdbhms.occupancy.domain.valueObjects.ReadingStatus.VOIDED
            ORDER BY reading.readingPeriod DESC, reading.readingDate DESC, reading.createdAt DESC, reading.id DESC
            """)
    List<MeterReadingEntity> findActiveByRoomIdLatestFirst(@Param("roomId") Long roomId);

    @Query("""
        SELECT r FROM MeterReadingEntity r
        JOIN FETCH r.room rm
        WHERE (:propertyId IS NULL OR rm.property.id = :propertyId)
          AND r.readingDate = (
              SELECT MAX(r2.readingDate)
              FROM MeterReadingEntity r2
              WHERE r2.room.id = rm.id AND r2.meter.meterType = r.meter.meterType
                AND r2.readingDate < :startOfPeriod
          )
    """)
    List<MeterReadingEntity> findLatestBeforeDateByProperty(
            @Param("propertyId") Long propertyId, 
            @Param("startOfPeriod") java.time.LocalDate startOfPeriod
    );
}
