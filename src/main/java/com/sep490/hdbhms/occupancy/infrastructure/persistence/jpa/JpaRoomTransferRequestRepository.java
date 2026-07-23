package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.domain.value_objects.TransferRequestStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomTransferRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaRoomTransferRequestRepository extends JpaRepository<RoomTransferRequestEntity, Long> {
    @Query("""
            select coalesce(sum(r.reservedSlots), 0)
            from RoomTransferRequestEntity r
            where r.targetRoom.id = :roomId
              and r.status in :statuses
              and r.reservedSlots is not null
              and r.reservedSlots > 0
              and (r.reservationExpiresAt is null or r.reservationExpiresAt >= :now)
              and (:excludedRequestId is null or r.id <> :excludedRequestId)
            """)
    long sumActiveReservedSlotsByRoomId(
            @Param("roomId") Long roomId,
            @Param("statuses") Collection<TransferRequestStatus> statuses,
            @Param("now") LocalDateTime now,
            @Param("excludedRequestId") Long excludedRequestId
    );

    boolean existsByOldContract_IdAndStatusIn(Long oldContractId, Collection<TransferRequestStatus> statuses);

    @Query("""
            SELECT r FROM RoomTransferRequestEntity r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:floorId IS NULL OR r.oldRoom.floor.id = :floorId OR r.targetRoom.floor.id = :floorId)
              AND (:roomId IS NULL OR r.oldRoom.id = :roomId OR r.targetRoom.id = :roomId)
              AND (
                    :fromDateTime IS NULL
                    OR (r.completedAt IS NOT NULL AND r.completedAt >= :fromDateTime)
                    OR (r.completedAt IS NULL AND r.executedAt IS NOT NULL AND r.executedAt >= :fromDateTime)
                    OR (r.completedAt IS NULL AND r.executedAt IS NULL AND r.requestedTransferDate >= :fromDate)
              )
              AND (
                    :toDateTime IS NULL
                    OR (r.completedAt IS NOT NULL AND r.completedAt <= :toDateTime)
                    OR (r.completedAt IS NULL AND r.executedAt IS NOT NULL AND r.executedAt <= :toDateTime)
                    OR (r.completedAt IS NULL AND r.executedAt IS NULL AND r.requestedTransferDate <= :toDate)
              )
            """)
    Page<RoomTransferRequestEntity> findHistory(
            @Param("status") TransferRequestStatus status,
            @Param("floorId") Long floorId,
            @Param("roomId") Long roomId,
            @Param("fromDate") LocalDate fromDate,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDate") LocalDate toDate,
            @Param("toDateTime") LocalDateTime toDateTime,
            Pageable pageable
    );

    List<RoomTransferRequestEntity> findByStatusAndUpdatedAtBefore(
            TransferRequestStatus status,
            LocalDateTime updatedBefore
    );

    // Find pending target holder approvals for a specific user
    @Query("""
            SELECT r FROM RoomTransferRequestEntity r
            WHERE r.status = :status
              AND r.targetContract IS NOT NULL
              AND r.targetContract.primaryTenantProfile.user.id = :holderUserId
            ORDER BY r.createdAt DESC
            """)
    List<RoomTransferRequestEntity> findPendingTargetHolderApprovals(
            @Param("status") TransferRequestStatus status,
            @Param("holderUserId") Long holderUserId
    );

    // Find pending source holder nominations for a specific user
    @Query("""
            SELECT r FROM RoomTransferRequestEntity r
            WHERE r.status = :status
              AND r.nominatedHolderProfile IS NOT NULL
              AND r.nominatedHolderProfile.user.id = :holderUserId
            ORDER BY r.createdAt DESC
            """)
    List<RoomTransferRequestEntity> findPendingHolderNominations(
            @Param("status") TransferRequestStatus status,
            @Param("holderUserId") Long holderUserId
    );

    Optional<RoomTransferRequestEntity> findByRequestCode(String requestCode);
}
