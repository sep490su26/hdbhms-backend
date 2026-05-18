package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.LeaseContractPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataLeaseContractRepository implements LeaseContractRepository {
    JpaLeaseContractRepository jpaLeaseContractRepository;
    LeaseContractPersistenceMapper leaseContractPersistenceMapper;

    @Override
    public LeaseContract save(LeaseContract leaseContract) {
        return leaseContractPersistenceMapper.toDomain(
                jpaLeaseContractRepository.save(
                        leaseContractPersistenceMapper.toEntity(leaseContract)
                )
        );
    }

    @Override
    public Optional<LeaseContract> findById(Long id) {
        return jpaLeaseContractRepository.findById(id)
                .map(leaseContractPersistenceMapper::toDomain);
    }
}
