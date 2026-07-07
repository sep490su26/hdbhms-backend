package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.domain.valueObjects.LeaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface JpaLeaseContractRepository extends JpaRepository<LeaseContractEntity, Long>, JpaSpecificationExecutor<LeaseContractEntity> {
    List<LeaseContractEntity> findAllByPrimaryTenantProfile_Id(Long tenantPersonProfileId);

    boolean existsByRoom_IdAndStatusInAndDeletedAtIsNull(Long roomId, List<LeaseStatus> statuses);

    boolean existsByContractCodeAndDeletedAtIsNull(String contractCode);

    boolean existsByPreviousContract_IdAndDeletedAtIsNull(Long previousContractId);

    boolean existsByPreviousContract_IdAndStatusAndDeletedAtIsNull(
            Long previousContractId,
            LeaseStatus status
    );

    Optional<LeaseContractEntity> findFirstByPreviousContract_IdAndDeletedAtIsNullOrderByIdDesc(Long previousContractId);

    List<LeaseContractEntity> findAllByStatusInAndDeletedAtIsNull(List<LeaseStatus> statuses);

    Optional<LeaseContractEntity> findFirstByRoom_IdAndStatusInAndDeletedAtIsNullOrderByIdDesc(
            Long roomId,
            List<LeaseStatus> statuses
    );

    boolean existsByPrimaryTenantProfile_Id(Long tenantProfileId);

    Optional<LeaseContractEntity> findByIdAndDeletedAtIsNull(Long id);
}
