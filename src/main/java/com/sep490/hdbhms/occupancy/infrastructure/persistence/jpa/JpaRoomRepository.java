package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaRoomRepository extends JpaRepository<RoomEntity, Long>, JpaSpecificationExecutor<RoomEntity> {
    @Query("""
                SELECT r FROM RoomEntity r
                JOIN FETCH r.property p
                JOIN FETCH r.floor f
                LEFT JOIN FETCH r.images img
                WHERE r.roomCode = :roomCode
            """)
    Optional<RoomEntity> findByRoomCodeWithDetails(@Param("roomCode") String roomCode);

    List<RoomEntity> findAllByProperty_IdAndFloor_Id(Long propertyId, Long floorId);

    List<RoomEntity> findAllByProperty_IdAndDeletedAtIsNullOrderBySortOrderAscRoomCodeAsc(Long propertyId);
}
