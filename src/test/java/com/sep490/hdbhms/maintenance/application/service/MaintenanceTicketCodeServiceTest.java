package com.sep490.hdbhms.maintenance.application.service;

import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceTicketRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaintenanceTicketCodeServiceTest {

    @Test
    void returnsBaseCodeWhenItIsAvailable() {
        MaintenanceTicketRepository maintenanceTicketRepository = mock(MaintenanceTicketRepository.class);
        when(maintenanceTicketRepository.existsByTicketCode("SC_P401_12_08_2026"))
                .thenReturn(false);

        assertEquals(
                "SC_P401_12_08_2026",
                new MaintenanceTicketCodeService(maintenanceTicketRepository)
                        .nextCode("401", LocalDate.of(2026, 8, 12))
        );
    }

    @Test
    void appendsSequenceWhenBaseCodeAlreadyExists() {
        MaintenanceTicketRepository maintenanceTicketRepository = mock(MaintenanceTicketRepository.class);
        when(maintenanceTicketRepository.existsByTicketCode("SC_P401_12_08_2026"))
                .thenReturn(true);
        when(maintenanceTicketRepository.existsByTicketCode("SC_P401_12_08_2026-2"))
                .thenReturn(true);
        when(maintenanceTicketRepository.existsByTicketCode("SC_P401_12_08_2026-3"))
                .thenReturn(false);

        assertEquals(
                "SC_P401_12_08_2026-3",
                new MaintenanceTicketCodeService(maintenanceTicketRepository)
                        .nextCode("401", LocalDate.of(2026, 8, 12))
        );
    }
}
