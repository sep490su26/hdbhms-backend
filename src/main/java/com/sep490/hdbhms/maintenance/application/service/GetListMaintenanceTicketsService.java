package com.sep490.hdbhms.maintenance.application.service;

import com.sep490.hdbhms.maintenance.application.port.in.query.GetListMaintenanceTicketsQuery;
import com.sep490.hdbhms.maintenance.application.port.in.usecase.GetListMaintenanceTicketsUseCase;
import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceTicketRepository;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicket;
import com.sep490.hdbhms.shared.utils.StringUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetListMaintenanceTicketsService implements GetListMaintenanceTicketsUseCase {
    MaintenanceTicketRepository maintenanceTicketRepository;

    @Override
    public Page<MaintenanceTicket> execute(GetListMaintenanceTicketsQuery query) {
        List<Long> ids = Collections.emptyList();
        if (!StringUtils.isEmpty(query.code())) {
            ids = maintenanceTicketRepository.findIdsByTicketCode(query.code());
            if (ids.isEmpty()) {
                return Page.empty(query.pageable());
            }
        }
        return maintenanceTicketRepository.findAll(
                ids,
                query.type(),
                query.status(),
                query.pageable()
        );
    }
}
