package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.application.port.in.query.GetPropertyRulesQuery;
import com.sep490.hdbhms.property.domain.model.PropertyRule;

import java.util.List;

public interface GetPropertyRulesUseCase {
    List<PropertyRule> execute(GetPropertyRulesQuery query);
}
