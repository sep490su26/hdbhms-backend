package com.sep490.hdbhms.property.infrastructure.persistence.jpa;

import com.sep490.hdbhms.property.infrastructure.persistence.entity.RuleViolationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRuleViolationRepository extends JpaRepository<RuleViolationEntity, Long> {
}
