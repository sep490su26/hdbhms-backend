package com.sep490.hdbhms.occupancy.application.port.in.query;

import com.sep490.hdbhms.occupancy.domain.valueObjects.LeaseStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public record GetListLeaseContractsQuery(
        Long userId,
        LeaseStatus status,
        LocalDateTime signedFrom,
        LocalDateTime signedTo,
        Pageable pageable
) {
}
