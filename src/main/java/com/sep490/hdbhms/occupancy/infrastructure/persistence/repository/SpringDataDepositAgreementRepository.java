package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositAgreementEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositAgreementRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.DepositAgreementPersistenceMapper;
import com.sep490.hdbhms.shared.specifications.DepositAgreementSpecifications;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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

    @Override
    public List<DepositAgreement> findAll() {
        return jpaDepositAgreementRepository.findAll().stream()
                .map(depositAgreementPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Page<DepositAgreement> findAll(List<Long> ids, DepositAgreementStatus status, LocalDateTime signedFrom, LocalDateTime signedTo, Pageable pageable) {
        Specification<DepositAgreementEntity> specification = Specification
                .where(DepositAgreementSpecifications.idIn(ids))
                .and(DepositAgreementSpecifications.statusIn(status))
                .and(DepositAgreementSpecifications.signingDateBetween(signedFrom, signedTo));
        return jpaDepositAgreementRepository.findAll(specification, pageable)
                .map(depositAgreementPersistenceMapper::toDomain);
    }

    @Override
    public List<DepositAgreement> findAllByTenantId(Long tenantId) {
        return jpaDepositAgreementRepository.findAllByTenant_Id(tenantId).stream()
                .map(depositAgreementPersistenceMapper::toDomain)
                .toList();
    }
}
