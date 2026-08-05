package com.sep490.hdbhms.booking.application.port.out;

import com.sep490.hdbhms.booking.domain.model.DepositTransferRecord;

import java.util.Optional;

public interface DepositTransferRecordRepository {
    DepositTransferRecord save(DepositTransferRecord depositTransferRecord);

    Optional<DepositTransferRecord> findByTransferRequestId(Long transferRequestId);

    Optional<DepositTransferRecord> findByNewContractId(Long newContractId);
}
