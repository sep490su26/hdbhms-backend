package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.application.port.in.query.GetPropertyDetailsQuery;
import com.sep490.hdbhms.property.domain.model.Property;

public interface GetPropertyDetailsUseCase {
    Property execute(GetPropertyDetailsQuery query);
}
