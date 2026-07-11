package com.sep490.hdbhms.maintenance.infrastructure.web.dto.request;

import com.sep490.hdbhms.maintenance.domain.value_objects.AttachmentPhase;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttachMaintenanceTicketFilesRequest {
    List<Long> fileIds;

    AttachmentPhase phase;

    String note;
}
