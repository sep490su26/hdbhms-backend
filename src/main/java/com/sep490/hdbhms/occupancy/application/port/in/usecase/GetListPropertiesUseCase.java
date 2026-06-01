package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetListPropertiesQuery;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import org.springframework.data.domain.Page;

public interface GetListPropertiesUseCase {
    Page<Property> execute(GetListPropertiesQuery query);
}
