package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractOccupantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface JpaContractOccupantRepository extends JpaRepository<ContractOccupantEntity, Long> {
    Optional<ContractOccupantEntity> findFirstByContract_IdAndTenantProfile_IdAndStatus(
            Long contractId,
            Long tenantProfileId,
            OccupantStatus status
    );

    List<ContractOccupantEntity> findAllByContract_IdAndStatus(Long contractId, OccupantStatus status);

    @Query("""
            select count(o)
            from ContractOccupantEntity o
            where o.status = com.sep490.hdbhms.occupancy.domain.value_objects.OccupantStatus.ACTIVE
              and o.contract.room.id = :roomId
              and o.contract.status in (
                    com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus.ACTIVE,
                    com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus.SIGNED,
                    com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus.CONFIRMED
              )
            """)
    long countActiveOccupantsByRoomId(Long roomId);
}
