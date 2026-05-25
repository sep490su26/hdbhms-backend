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
public class VisitRequestResponse {
    Long id;
    String customerName;
    String phone;
    Long propertyId;
    String propertyName;
    Long roomId;
    String roomCode;
    LocalDateTime appointmentAt;
    VisitRequestStatus status;
    String statusLabel;
    VisitRequestSource source;
    String note;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;
}
