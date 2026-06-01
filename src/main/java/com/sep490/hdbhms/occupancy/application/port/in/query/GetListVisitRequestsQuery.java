package com.sep490.hdbhms.occupancy.application.port.in.query;

import com.sep490.hdbhms.occupancy.domain.value_objects.VisitRequestStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public record GetListVisitRequestsQuery(
        String keyword,
        String propertyCode,
        String roomCode,
        Long propertyId,
        Long roomId,
        VisitRequestStatus status,
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
) {
}
