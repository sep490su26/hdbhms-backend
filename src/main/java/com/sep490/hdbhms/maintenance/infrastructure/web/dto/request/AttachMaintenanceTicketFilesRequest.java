package com.sep490.hdbhms.maintenance.infrastructure.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias({"fileIds", "file_ids", "attachmentIds", "attachment_ids"})
    List<Long> fileIds;

    @JsonAlias({"phase", "attachmentPhase", "attachment_phase"})
    AttachmentPhase phase;

    String note;
}
