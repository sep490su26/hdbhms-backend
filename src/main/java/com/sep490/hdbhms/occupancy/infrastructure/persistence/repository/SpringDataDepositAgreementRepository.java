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
import org.springframework.jdbc.core.JdbcTemplate;
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
    JdbcTemplate jdbcTemplate;

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

    @Override
    public List<DepositAgreement> findAllAccessibleByUserId(Long userId) {
        List<Long> ids = jdbcTemplate.query("""
                        SELECT DISTINCT da.deposit_agreement_id AS id
                        FROM deposit_agreements da
                        JOIN users u ON u.user_id = ?
                        LEFT JOIN tenants t ON t.tenant_id = da.tenant_id
                        LEFT JOIN person_profiles depositor_pp ON depositor_pp.person_profile_id = da.depositor_person_profile_id
                        LEFT JOIN lease_contracts lc
                          ON lc.deposit_agreement_id = da.deposit_agreement_id
                         AND lc.deleted_at IS NULL
                        LEFT JOIN person_profiles primary_pp ON primary_pp.person_profile_id = lc.primary_tenant_profile_id
                        LEFT JOIN contract_occupants co
                          ON co.contract_id = lc.lease_contract_id
                         AND co.status = 'ACTIVE'
                        LEFT JOIN person_profiles occupant_pp ON occupant_pp.person_profile_id = co.tenant_profile_id
                        WHERE u.deleted_at IS NULL
                          AND (
                              t.user_id = u.user_id
                              OR depositor_pp.user_id = u.user_id
                              OR primary_pp.user_id = u.user_id
                              OR occupant_pp.user_id = u.user_id
                              OR (u.phone IS NOT NULL AND (
                                  depositor_pp.phone = u.phone
                                  OR primary_pp.phone = u.phone
                                  OR occupant_pp.phone = u.phone
                              ))
                              OR (u.email IS NOT NULL AND (
                                  LOWER(depositor_pp.email) = LOWER(u.email)
                                  OR LOWER(primary_pp.email) = LOWER(u.email)
                                  OR LOWER(occupant_pp.email) = LOWER(u.email)
                              ))
                          )
                        """,
                (rs, rowNum) -> rs.getLong("id"),
                userId
        );
        if (ids.isEmpty()) {
            return List.of();
        }
        return jpaDepositAgreementRepository.findAllById(ids).stream()
                .map(depositAgreementPersistenceMapper::toDomain)
                .toList();
    }
}
