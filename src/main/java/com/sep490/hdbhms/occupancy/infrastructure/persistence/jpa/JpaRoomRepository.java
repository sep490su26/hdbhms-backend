package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface JpaRoomRepository extends JpaRepository<RoomEntity, Long>, JpaSpecificationExecutor<RoomEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RoomEntity r JOIN FETCH r.property WHERE r.id IN :roomIds ORDER BY r.id")
    List<RoomEntity> findAllByIdForUpdate(@Param("roomIds") List<Long> roomIds);

    @Query("""
                SELECT r FROM RoomEntity r
                JOIN FETCH r.property p
                JOIN FETCH r.floor f
                LEFT JOIN FETCH r.images img
                WHERE r.roomCode = :roomCode
            """)
    Optional<RoomEntity> findByRoomCodeWithDetails(@Param("roomCode") String roomCode);

    List<RoomEntity> findAllByProperty_IdAndFloor_Id(Long propertyId, Long floorId);

    List<RoomEntity> findAllByProperty_IdAndFloor_IdAndDeletedAtIsNull(Long propertyId, Long floorId);

    List<RoomEntity> findAllByProperty_Id(Long propertyId);

    List<RoomEntity> findAllByProperty_IdAndDeletedAtIsNull(Long propertyId);

    List<RoomEntity> findAllByFloor_IdAndDeletedAtIsNull(Long floorId);

    List<RoomEntity> findAllByProperty_IdAndDeletedAtIsNullOrderBySortOrderAscRoomCodeAsc(Long propertyId);

    long countByFloor_Id(Long floorId);

    Optional<RoomEntity> findByRoomCode(String roomCode);

    boolean existsByProperty_IdAndRoomCodeAndDeletedAtIsNull(Long propertyId, String roomCode);

    @Modifying
    @Query("UPDATE RoomEntity r SET r.currentStatus = :newStatus, r.version = r.version + 1 " +
            "WHERE r.id = :roomId AND r.currentStatus = :expectedStatus")
    int updateRoomStatusIfCurrent(@Param("roomId") Long roomId,
                                  @Param("expectedStatus") RoomStatus expected,
                                  @Param("newStatus") RoomStatus newStatus);
}
