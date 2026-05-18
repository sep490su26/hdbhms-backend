package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.RentOverrideEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRentOverrideRepository extends JpaRepository<RentOverrideEntity, Long> {
}
