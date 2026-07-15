package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.domain.value_objects.UtilityType;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.UtilityTariffEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface JpaUtilityTariffRepository extends JpaRepository<UtilityTariffEntity, Long> {

    @Query("""
            SELECT tariff
            FROM UtilityTariffEntity tariff
            WHERE tariff.property.id = :propertyId
              AND tariff.utilityType = :utilityType
              AND tariff.effectiveFrom <= :date
              AND (tariff.effectiveTo IS NULL OR tariff.effectiveTo >= :date)
            ORDER BY tariff.effectiveFrom DESC, tariff.id DESC
            """)
    List<UtilityTariffEntity> findEffectiveTariffs(
            @Param("propertyId") Long propertyId,
            @Param("utilityType") UtilityType utilityType,
            @Param("date") LocalDate date
    );
}
