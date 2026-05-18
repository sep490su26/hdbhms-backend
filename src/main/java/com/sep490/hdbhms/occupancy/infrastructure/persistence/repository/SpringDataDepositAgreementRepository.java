package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositAgreementRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.DepositAgreementPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataDepositAgreementRepository implements DepositAgreementRepository {
    JpaDepositAgreementRepository jpaDepositAgreementRepository;
    DepositAgreementPersistenceMapper depositAgreementPersistenceMapper;

    @Override
    public DepositAgreement save(DepositAgreement depositAgreement) {
        return depositAgreementPersistenceMapper.toDomain(
                jpaDepositAgreementRepository.save(
                        depositAgreementPersistenceMapper.toEntity(
                                depositAgreement
                        )
                )
        );
    }

    @Override
    public Optional<DepositAgreement> findById(Long id) {
        return jpaDepositAgreementRepository.findById(id)
                .map(depositAgreementPersistenceMapper::toDomain);
    }
}
