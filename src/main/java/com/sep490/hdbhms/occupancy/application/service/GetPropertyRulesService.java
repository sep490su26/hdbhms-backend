package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetPropertyRulesQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetPropertyRulesUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.PropertyRuleRepository;
import com.sep490.hdbhms.occupancy.domain.model.PropertyRule;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetPropertyRulesService implements GetPropertyRulesUseCase {
    PropertyRuleRepository propertyRuleRepository;

    @Override
    public List<PropertyRule> execute(GetPropertyRulesQuery query) {
        return propertyRuleRepository.findActiveByPropertyId(query.propertyId());
    }
}
