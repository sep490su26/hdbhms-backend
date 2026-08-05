package com.sep490.hdbhms.property.infrastructure.persistence.jpa;

import com.sep490.hdbhms.property.infrastructure.persistence.entity.PropertyRuleEntity;
import com.sep490.hdbhms.property.domain.value_objects.RuleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaPropertyRuleRepository extends JpaRepository<PropertyRuleEntity, Long> {
    Optional<PropertyRuleEntity> findFirstByProperty_IdAndRuleCodeAndStatus(
            Long propertyId,
            String ruleCode,
            RuleStatus status
    );

    List<PropertyRuleEntity> findAllByProperty_IdAndStatusOrderBySortOrderAscRuleCodeAsc(
            Long propertyId,
            RuleStatus status
    );

    Optional<PropertyRuleEntity> findFirstByProperty_IdAndRuleCode(Long propertyId, String ruleCode);

    Optional<PropertyRuleEntity> findByIdAndProperty_Id(Long id, Long propertyId);
}
