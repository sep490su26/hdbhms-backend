package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.valueObjects.VisitRequestStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VisitRequestDetailsResponse {
    Long id;
    PropertyResponse property;
    RoomResponse room;
    String visitorName;
    String visitorPhone;
    String visitorEmail;
    LocalDateTime preferredStart;
    VisitRequestStatus status;
    String notes;
    LocalDateTime createdAt;
    LocalDateTime deletedAt;
}
