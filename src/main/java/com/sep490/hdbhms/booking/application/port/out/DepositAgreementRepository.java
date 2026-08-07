package com.sep490.hdbhms.booking.application.port.out;

import com.sep490.hdbhms.booking.domain.model.DepositAgreement;

import java.util.List;
import java.util.Optional;

public interface DepositAgreementRepository {
    DepositAgreement save(DepositAgreement depositAgreement);

    Optional<DepositAgreement> findById(Long id);

    default boolean existsByDepositCode(String depositCode) {
        return false;
    }

    List<DepositAgreement> findAllByTenantId(Long tenantId);

    List<DepositAgreement> findAllAccessibleByUserId(Long userId);
}
