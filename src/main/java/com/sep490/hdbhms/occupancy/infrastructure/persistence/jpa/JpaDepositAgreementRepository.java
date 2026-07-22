package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositAgreementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;

public interface JpaDepositAgreementRepository extends JpaRepository<DepositAgreementEntity, Long>, JpaSpecificationExecutor<DepositAgreementEntity> {
    List<DepositAgreementEntity> findAllByTenant_Id(Long tenantId);

    boolean existsByDepositCode(String depositCode);

    @Query("select count(d) from DepositAgreementEntity d where d.id in :ids and d.status in :statuses")
    long countByIdsAndStatuses(@Param("ids") List<Long> ids, @Param("statuses") List<DepositAgreementStatus> statuses);

    @Query("select coalesce(sum(d.amount), 0) from DepositAgreementEntity d where d.id in :ids and d.status in :statuses")
    Long sumAmountByIdsAndStatuses(@Param("ids") List<Long> ids, @Param("statuses") List<DepositAgreementStatus> statuses);

    @Query("select distinct d.room.floor.id from DepositAgreementEntity d where d.id in :ids and d.status in :statuses order by d.room.floor.id")
    List<Long> findDistinctFloorIds(@Param("ids") List<Long> ids, @Param("statuses") List<DepositAgreementStatus> statuses);
}
