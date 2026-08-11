package com.sep490.hdbhms.property.infrastructure.persistence.jpa;

import com.sep490.hdbhms.property.infrastructure.persistence.entity.PropertyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface JpaPropertyRepository extends JpaRepository<PropertyEntity, Long>, JpaSpecificationExecutor<PropertyEntity> {
    List<PropertyEntity> findAllByDeletedAtIsNull();

    List<PropertyEntity> findAllByIdInAndDeletedAtIsNull(List<Long> ids);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);
}
