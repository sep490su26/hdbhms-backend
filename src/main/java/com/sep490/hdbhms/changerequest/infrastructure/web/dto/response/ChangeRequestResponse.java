package com.sep490.hdbhms.changerequest.infrastructure.web.dto.response;

import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;

import java.time.LocalDateTime;

public record ChangeRequestResponse(
        Long id,
        String requestCode,
        RequestType requestType,
        TargetType targetType,
        Long targetId,
        String title,
        String description,
        String requestPayload,
        RequestStatus status,
        Long requesterId,
        String resolutionNote,
        LocalDateTime createdAt
) {}
