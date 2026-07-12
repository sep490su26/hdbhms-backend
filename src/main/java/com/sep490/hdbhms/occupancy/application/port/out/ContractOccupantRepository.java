package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.ContractOccupant;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContractOccupantRepository {
    void saveAll(List<ContractOccupant> oldOccupants);

    List<ContractOccupant> findAllByContractIdAndStatus(Long contractId, OccupantStatus occupantStatus);

    long countActiveOccupantsByRoomId(Long id);

    Optional<ContractOccupant> findFirstByContract_IdAndTenantProfile_IdAndStatus(Long id, Long requesterProfileId, OccupantStatus occupantStatus);
}
