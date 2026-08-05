package com.sep490.hdbhms.booking.infrastructure.persistence.jpa;

import com.sep490.hdbhms.booking.infrastructure.persistence.entity.DepositContactEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaDepositContactEventRepository extends JpaRepository<DepositContactEventEntity, Long> {
    Optional<DepositContactEventEntity> findFirstByDepositAgreement_IdOrderByContactedAtDescIdDesc(Long depositAgreementId);
}
