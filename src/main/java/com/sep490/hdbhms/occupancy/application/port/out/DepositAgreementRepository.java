package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;

import java.util.Optional;

public interface DepositAgreementRepository {
    DepositAgreement save(DepositAgreement depositAgreement);

    Optional<DepositAgreement> findById(Long id);
}
