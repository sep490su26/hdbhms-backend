package com.sep490.hdbhms.occupancy.application.port.in.query;

import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyStatus;
import org.springframework.data.domain.Pageable;

public record GetListPropertiesQuery(PropertyStatus status, Pageable pageable) {
}
