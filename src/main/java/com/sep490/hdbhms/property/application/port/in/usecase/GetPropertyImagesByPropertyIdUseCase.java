package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.application.port.in.query.GetPropertyImagesByPropertyIdQuery;
import com.sep490.hdbhms.property.domain.model.PropertyImage;

import java.util.List;

public interface GetPropertyImagesByPropertyIdUseCase {
    List<PropertyImage> execute(GetPropertyImagesByPropertyIdQuery query);
}
