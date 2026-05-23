package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.Property;

import java.util.Optional;

public interface PropertyRepository {
    Property save(Property property);

    Optional<Property> findById(Long id);
}
