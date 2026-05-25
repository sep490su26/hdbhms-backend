package com.sep490.hdbhms.maintenance.domain.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaintenanceTicketEvent {
    Long id;
    Long ticketId;
    String fromStatus;
    String toStatus;
    String note;
    Long createdById;
    LocalDateTime createdAt;
}