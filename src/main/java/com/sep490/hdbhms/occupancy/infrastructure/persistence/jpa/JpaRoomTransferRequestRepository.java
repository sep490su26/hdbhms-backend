package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.domain.value_objects.TransferRequestStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomTransferRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
