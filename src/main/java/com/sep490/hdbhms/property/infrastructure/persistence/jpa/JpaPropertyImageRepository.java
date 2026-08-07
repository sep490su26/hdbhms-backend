package com.sep490.hdbhms.property.infrastructure.persistence.jpa;

import com.sep490.hdbhms.property.infrastructure.persistence.entity.PropertyImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaPropertyImageRepository extends JpaRepository<PropertyImageEntity, Long> {
    List<PropertyImageEntity> findAllByProperty_IdOrderBySortOrderAscCreatedAtAscIdAsc(Long propertyId);
}
