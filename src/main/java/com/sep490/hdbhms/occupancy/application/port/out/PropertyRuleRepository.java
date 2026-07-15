package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.PropertyRule;

import java.util.List;

public interface PropertyRuleRepository {
    List<PropertyRule> findActiveByPropertyId(Long propertyId);
}
