package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;

import java.util.Optional;

public interface VisitRequestRepository {
    VisitRequest save(VisitRequest visitRequest);
    Optional<VisitRequest> findById(Long id);
}
