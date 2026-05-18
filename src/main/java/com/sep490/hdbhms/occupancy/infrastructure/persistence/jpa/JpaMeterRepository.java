package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMeterRepository extends JpaRepository<MeterEntity, Long> {
}
