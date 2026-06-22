package com.sep490.hdbhms.maintenance.application.port.out;

import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface MaintenanceTicketRepository {
    MaintenanceTicket save(MaintenanceTicket maintenanceTicket);

    Optional<MaintenanceTicket> findById(Long id);

    List<Long> findIdsByTicketCode(String code);

    Page<MaintenanceTicket> findAll(
            List<Long> ids,
            String type,
            String status,
            Long roomId,
            Pageable pageable
    );
}
