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
public class VisitRequestResponse {
    Long id;
    Long propertyId;
    Long roomId;
    String visitorName;
    String visitorPhone;
    String visitorEmail;
    String propertyName;
    String roomName;
    LocalDateTime preferredStart;
    VisitRequestStatus status;
    String notes;
    LocalDateTime createdAt;
    LocalDateTime deletedAt;
}
