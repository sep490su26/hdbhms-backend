package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.DepositForm;

import java.util.Optional;

public interface DepositFormRepository {
    Optional<DepositForm> findById(Long id);

    DepositForm save(DepositForm depositForm);
}
