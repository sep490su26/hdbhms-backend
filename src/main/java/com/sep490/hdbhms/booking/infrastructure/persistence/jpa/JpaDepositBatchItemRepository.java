package com.sep490.hdbhms.booking.infrastructure.persistence.jpa;

import com.sep490.hdbhms.booking.infrastructure.persistence.entity.DepositBatchItemEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaDepositBatchItemRepository extends JpaRepository<DepositBatchItemEntity, Long> {
    @EntityGraph(attributePaths = {"room", "roomHold", "depositForm"})
    List<DepositBatchItemEntity> findAllByBatch_IdOrderByRoom_RoomCodeAsc(Long batchId);
}
