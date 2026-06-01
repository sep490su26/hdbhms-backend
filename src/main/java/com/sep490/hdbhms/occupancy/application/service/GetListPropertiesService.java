package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetListPropertiesQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetListPropertiesUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.PropertyRepository;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetListPropertiesService implements GetListPropertiesUseCase {
    PropertyRepository propertyRepository;

    @Override
    public Page<Property> execute(GetListPropertiesQuery query) {
        return propertyRepository.findAll(query.status(), query.pageable());
    }
}
