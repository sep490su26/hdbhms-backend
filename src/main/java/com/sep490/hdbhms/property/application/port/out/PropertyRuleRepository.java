package com.sep490.hdbhms.property.application.port.out;

import com.sep490.hdbhms.property.domain.model.PropertyRule;

import java.util.List;

public interface PropertyRuleRepository {
    List<PropertyRule> findActiveByPropertyId(Long propertyId);
}
