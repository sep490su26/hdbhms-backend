package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.ContractOccupantRepository;
import com.sep490.hdbhms.occupancy.domain.model.ContractOccupant;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractOccupantRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.ContractOccupantPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataContractOccupantRepository implements ContractOccupantRepository {
    JpaContractOccupantRepository jpaContractOccupantRepository;
    ContractOccupantPersistenceMapper contractOccupantPersistenceMapper;

    @Override
    public void saveAll(List<ContractOccupant> oldOccupants) {
        jpaContractOccupantRepository.saveAll(
                oldOccupants.stream()
                        .map(contractOccupantPersistenceMapper::toEntity)
                        .toList()
        );
    }

    @Override
    public List<ContractOccupant> findAllByContractIdAndStatus(Long contractId, OccupantStatus occupantStatus) {
        return jpaContractOccupantRepository.findAllByContract_IdAndStatus(contractId, occupantStatus).stream()
                .map(contractOccupantPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public long countActiveOccupantsByRoomId(Long id) {
        return jpaContractOccupantRepository.countActiveOccupantsByRoomId(id);
    }

    @Override
    public Optional<ContractOccupant> findFirstByContract_IdAndTenantProfile_IdAndStatus(Long id, Long requesterProfileId, OccupantStatus occupantStatus) {
        return jpaContractOccupantRepository.findFirstByContract_IdAndTenantProfile_IdAndStatus(id, requesterProfileId, occupantStatus)
                .map(contractOccupantPersistenceMapper::toDomain);
    }
}
