package com.sep490.hdbhms.maintenance.application.port.in.query;


import org.springframework.data.domain.Pageable;

public record GetListMaintenanceTicketsQuery(
        String code,
        String type,
        String status,
        Long roomId,
        Pageable pageable
) {
}
