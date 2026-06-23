package com.sep490.hdbhms.maintenance.application.port.out;

import com.sep490.hdbhms.maintenance.domain.model.MaintenanceCost;

import java.util.List;

public interface MaintenanceCostRepository {
    MaintenanceCost save(MaintenanceCost maintenanceCost);

    List<MaintenanceCost> findAllByTicketId(Long ticketId);
}
