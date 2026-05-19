package com.sep490.hdbhms.maintenance.domain.model;

import com.sep490.hdbhms.maintenance.domain.value_objects.AttachmentPhase;
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
public class MaintenanceTicketAttachment {
    Long id;
    Long ticketId;
    Long fileId;
    AttachmentPhase attachmentPhase;
    Integer sortOrder;
    Long createdById;
    LocalDateTime createdAt;
}