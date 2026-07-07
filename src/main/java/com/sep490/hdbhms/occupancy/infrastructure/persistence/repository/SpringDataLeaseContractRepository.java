package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.valueObjects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.LeaseContractPersistenceMapper;
import com.sep490.hdbhms.shared.specifications.LeaseContractSpecifications;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Override
    public List<LeaseContract> findAllByTenantPersonProfileId(Long tenantPersonProfileId) {
        List<LeaseContractEntity> result = jpaLeaseContractRepository
                .findAllByPrimaryTenantProfile_Id(tenantPersonProfileId);
        if (result == null) {
            return new ArrayList<>();
        }
        return result.stream()
                .map(leaseContractPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<LeaseContract> findAll() {
        return jpaLeaseContractRepository.findAll().stream()
                .map(leaseContractPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Page<LeaseContract> findAll(List<Long> ids, LeaseStatus status, LocalDateTime signedFrom, LocalDateTime signedTo, Pageable pageable) {
        Specification<LeaseContractEntity> specification = Specification
                .where(LeaseContractSpecifications.idIn(ids))
                .and(LeaseContractSpecifications.statusIn(status))
                .and(LeaseContractSpecifications.signingDateBetween(signedFrom, signedTo));
        return jpaLeaseContractRepository.findAll(specification, pageable)
                .map(leaseContractPersistenceMapper::toDomain);
    }

    @Override
    public List<LeaseContract> findAllByTenantProfileId(Long id) {
        return jpaLeaseContractRepository.findAllByPrimaryTenantProfile_Id(id).stream()
                .map(leaseContractPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean isTenantHasAnyActiveContract(Long tenantProfileId) {
        return jpaLeaseContractRepository.existsByPrimaryTenantProfile_Id(tenantProfileId);
    }

    @Override
    public Optional<LeaseContract> findFirstActiveContract(Long roomId, List<LeaseStatus> statuses) {
        return jpaLeaseContractRepository.findFirstByRoom_IdAndStatusInAndDeletedAtIsNullOrderByIdDesc(roomId, statuses)
                .map(leaseContractPersistenceMapper::toDomain);
    }
}
