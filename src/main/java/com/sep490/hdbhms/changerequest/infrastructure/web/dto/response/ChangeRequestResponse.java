package com.sep490.hdbhms.changerequest.infrastructure.web.dto.response;

import com.sep490.hdbhms.changerequest.domain.valueObjects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.valueObjects.RequestType;

import java.time.LocalDateTime;

public record ChangeRequestResponse(
        Long id,
        String requestCode,
        RequestType requestType,
        String title,
        String description,
        RequestStatus status,
        Long requesterId,
        String resolutionNote,
        LocalDateTime createdAt
) {}
