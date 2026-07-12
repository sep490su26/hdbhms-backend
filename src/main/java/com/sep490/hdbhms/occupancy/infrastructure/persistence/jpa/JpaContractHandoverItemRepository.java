package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaContractHandoverItemRepository extends JpaRepository<ContractHandoverItemEntity, Long> {
    List<ContractHandoverItemEntity> findByHandoverRecord_Id(Long handoverRecordId);

    @Query("""
            SELECT item FROM ContractHandoverItemEntity item
            LEFT JOIN FETCH item.evidenceFile
            WHERE item.handoverRecord.id = :handoverRecordId
            ORDER BY item.id ASC
            """)
    List<ContractHandoverItemEntity> findWithEvidenceFileByHandoverRecordId(
            @Param("handoverRecordId") Long handoverRecordId
    );
}
