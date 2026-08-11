package com.sep490.hdbhms.property.application.port.out;

import com.sep490.hdbhms.property.domain.model.Property;
import com.sep490.hdbhms.property.domain.value_objects.PropertyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PropertyRepository {
    Property save(Property property);

    boolean existsByName(String name);

    Optional<Property> findById(Long id);

    Page<Property> findAll(PropertyStatus status, Pageable pageable);
}
