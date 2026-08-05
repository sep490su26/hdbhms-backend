package com.sep490.hdbhms.property.application.port.in.command;

import java.time.LocalDateTime;

public record CreateVisitRequestCommand(
        Long propertyId,
        Long roomId,
        String visitorName,
        String visitorPhone,
        String visitorEmail,
        LocalDateTime preferredStart,
        String notes
) {
}
