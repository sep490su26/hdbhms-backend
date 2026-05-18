package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.DepositFormRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositFormRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.DepositFormPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataDepositFormRepository implements DepositFormRepository {
    JpaDepositFormRepository jpaDepositFormRepository;
    DepositFormPersistenceMapper depositFormPersistenceMapper;

    @Override
    public Optional<DepositForm> findById(Long id) {
        return jpaDepositFormRepository.findById(id)
                .map(depositFormPersistenceMapper::toDomain);
    }

    @Override
    public DepositForm save(DepositForm depositForm) {
        return depositFormPersistenceMapper.toDomain(
                jpaDepositFormRepository.save(
                        depositFormPersistenceMapper.toEntity(depositForm)
                )
        );
    }
}
