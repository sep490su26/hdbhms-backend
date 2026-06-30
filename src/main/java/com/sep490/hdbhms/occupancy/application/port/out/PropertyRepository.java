package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.valueObjects.PropertyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PropertyRepository {
    Property save(Property property);

    Optional<Property> findById(Long id);

    Page<Property> findAll(PropertyStatus status, Pageable pageable);
}
