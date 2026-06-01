package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositAgreementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface JpaDepositAgreementRepository extends JpaRepository<DepositAgreementEntity, Long>, JpaSpecificationExecutor<DepositAgreementEntity> {
    List<DepositAgreementEntity> findAllByTenant_Id(Long tenantId);
}
