package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.PropertyRuleRepository;
import com.sep490.hdbhms.occupancy.domain.model.PropertyRule;
import com.sep490.hdbhms.occupancy.domain.value_objects.RuleStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRuleRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.PropertyRulePersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataPropertyRuleRepository implements PropertyRuleRepository {
    JpaPropertyRuleRepository jpaPropertyRuleRepository;
    PropertyRulePersistenceMapper propertyRulePersistenceMapper;

    @Override
    public List<PropertyRule> findActiveByPropertyId(Long propertyId) {
        return jpaPropertyRuleRepository
                .findAllByProperty_IdAndStatusOrderBySortOrderAscRuleCodeAsc(propertyId, RuleStatus.ACTIVE)
                .stream()
                .map(propertyRulePersistenceMapper::toDomain)
                .toList();
    }
}
