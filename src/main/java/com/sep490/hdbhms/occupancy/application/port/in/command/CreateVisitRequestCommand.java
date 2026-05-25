package com.sep490.hdbhms.occupancy.application.port.in.command;

import com.sep490.hdbhms.occupancy.domain.value_objects.VisitRequestSource;

import java.time.LocalDateTime;

public record CreateVisitRequestCommand(
        Long propertyId,
        Long roomId,
        String visitorName,
        String visitorPhone,
        String visitorEmail,
        LocalDateTime preferredStart,
        VisitRequestSource source,
        String notes
) {
}
