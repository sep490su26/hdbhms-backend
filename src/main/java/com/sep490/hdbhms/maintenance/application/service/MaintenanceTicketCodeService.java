package com.sep490.hdbhms.maintenance.application.service;

import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceTicketRepository;
import com.sep490.hdbhms.shared.utils.DocumentFilenameBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MaintenanceTicketCodeService {
    MaintenanceTicketRepository maintenanceTicketRepository;

    public String nextCode(String roomCode, LocalDate createdDate) {
        String baseCode = DocumentFilenameBuilder.buildMaintenanceTicketCode(roomCode, createdDate);
        if (!maintenanceTicketRepository.existsByTicketCode(baseCode)) {
            return baseCode;
        }

        int sequence = 2;
        String candidate;
        do {
            candidate = baseCode + "-" + sequence++;
        } while (maintenanceTicketRepository.existsByTicketCode(candidate));
        return candidate;
    }
}
