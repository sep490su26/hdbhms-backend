package com.sep490.hdbhms.booking.infrastructure.persistence.jpa;

import com.sep490.hdbhms.booking.infrastructure.persistence.entity.DepositFormEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaDepositAgreementRepository extends JpaRepository<DepositFormEntity, Long> {
    List<DepositFormEntity> findAllByTenant_Id(Long tenantId);

    boolean existsByDepositCode(String depositCode);
}
