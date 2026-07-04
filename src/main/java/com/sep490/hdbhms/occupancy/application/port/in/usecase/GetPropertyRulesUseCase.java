package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetPropertyRulesQuery;
import com.sep490.hdbhms.occupancy.domain.model.PropertyRule;

import java.util.List;

public interface GetPropertyRulesUseCase {
    List<PropertyRule> execute(GetPropertyRulesQuery query);
}
