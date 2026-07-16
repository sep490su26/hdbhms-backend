package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
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

    boolean existsByProperty_IdAndDeletedAtIsNull(Long propertyId);

    long countByFloor_Id(Long floorId);

    Optional<RoomEntity> findByRoomCode(String roomCode);

    boolean existsByProperty_IdAndRoomCodeAndDeletedAtIsNull(Long propertyId, String roomCode);

    @Modifying
    @Query("UPDATE RoomEntity r SET r.currentStatus = :newStatus, r.version = r.version + 1 " +
            "WHERE r.id = :roomId AND r.currentStatus = :expectedStatus")
    int updateRoomStatusIfCurrent(@Param("roomId") Long roomId,
                                  @Param("expectedStatus") RoomStatus expected,
                                  @Param("newStatus") RoomStatus newStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RoomEntity room
            SET room.currentStatus = :newStatus,
                room.version = room.version + 1
            WHERE room.property.id = :propertyId
              AND room.deletedAt IS NULL
              AND room.currentStatus = :expectedStatus
              AND NOT EXISTS (
                  SELECT contract.id FROM LeaseContractEntity contract
                  WHERE contract.room.id = room.id
                    AND contract.deletedAt IS NULL
                    AND contract.status IN :activeStatuses
              )
              AND NOT EXISTS (
                  SELECT deposit.id FROM DepositAgreementEntity deposit
                  WHERE deposit.room.id = room.id
                    AND deposit.deletedAt IS NULL
                    AND deposit.status IN :activeDepositStatuses
              )
            """)
    int updateDraftRoomsWithoutActiveCommitmentsToStatus(
            @Param("propertyId") Long propertyId,
            @Param("expectedStatus") RoomStatus expectedStatus,
            @Param("activeStatuses") List<LeaseStatus> activeStatuses,
            @Param("activeDepositStatuses") List<DepositAgreementStatus> activeDepositStatuses,
            @Param("newStatus") RoomStatus newStatus
    );
}
