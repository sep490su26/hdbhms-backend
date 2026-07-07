package com.sep490.hdbhms.changerequest.infrastructure.web.dto.response;

import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;

import java.time.LocalDateTime;

public record ChangeRequestResponse(
        Long id,
        String requestCode,
        RequestType requestType,
        String title,
        String description,
        RequestStatus status,
        Long requesterId,
        Long targetId,
        String resolutionNote,
        LocalDateTime createdAt
) {}
