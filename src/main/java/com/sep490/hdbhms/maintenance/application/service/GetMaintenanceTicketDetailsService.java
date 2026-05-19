package com.sep490.hdbhms.maintenance.application.service;

import com.sep490.hdbhms.maintenance.application.port.in.query.GetMaintenanceTicketDetailsQuery;
import com.sep490.hdbhms.maintenance.application.port.in.usecase.GetMaintenanceTicketDetailsUseCase;
import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceTicketRepository;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicket;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetMaintenanceTicketDetailsService implements GetMaintenanceTicketDetailsUseCase {
    MaintenanceTicketRepository maintenanceTicketRepository;

    @Override
    public MaintenanceTicket execute(GetMaintenanceTicketDetailsQuery query) {
        return maintenanceTicketRepository.findById(query.maintenanceTicketId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid maintenance ticket id: " + query.maintenanceTicketId()));
    }
}
