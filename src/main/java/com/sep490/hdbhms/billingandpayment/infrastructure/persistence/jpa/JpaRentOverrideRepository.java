package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.RentOverrideEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaRentOverrideRepository extends JpaRepository<RentOverrideEntity, Long> {
    Optional<RentOverrideEntity> findByContract_IdAndBillingPeriod(Long contractId, String billingPeriod);
}
