package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPropertyRuleRepository extends JpaRepository<PropertyRuleEntity, Long> {
}
