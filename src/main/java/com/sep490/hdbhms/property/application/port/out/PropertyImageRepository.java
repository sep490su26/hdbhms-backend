package com.sep490.hdbhms.property.application.port.out;

import com.sep490.hdbhms.property.domain.model.PropertyImage;

import java.util.List;
import java.util.Optional;

public interface PropertyImageRepository {
    PropertyImage save(PropertyImage propertyImage);

    Optional<PropertyImage> findById(Long id);

    List<PropertyImage> findAllByPropertyId(Long propertyId);

    void deleteById(Long id);
}
