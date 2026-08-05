package com.sep490.hdbhms.property.application.port.out;

import com.sep490.hdbhms.property.domain.model.Floor;

import java.util.List;
import java.util.Optional;

public interface FloorRepository {
    Floor save(Floor floor);

    Optional<Floor> findById(Long id);

    boolean existsActiveByPropertyIdAndFloorCode(Long propertyId, String floorCode);

    List<Floor> findAllByPropertyId(Long propertyId);
}
