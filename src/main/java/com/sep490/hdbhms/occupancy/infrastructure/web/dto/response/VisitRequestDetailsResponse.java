package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.value_objects.VisitRequestSource;
import com.sep490.hdbhms.occupancy.domain.value_objects.VisitRequestStatus;
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
    Long propertyId;
    String propertyName;
    Long roomId;
    String roomCode;
    String customerName;
    String phone;
    String visitorEmail;
    LocalDateTime appointmentAt;
    VisitRequestStatus status;
    String statusLabel;
    VisitRequestSource source;
    String note;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
