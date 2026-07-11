package com.sep490.hdbhms.changerequest.domain.model;

import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangeRequestEvent {
    Long id;
    Long requestId;
    RequestStatus fromStatus;
    RequestStatus toStatus;
    String note;
    Long actedBy;
    LocalDateTime actedAt;
}
