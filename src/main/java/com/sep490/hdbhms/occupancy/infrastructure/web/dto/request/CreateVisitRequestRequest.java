package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateVisitRequestRequest {
    Long propertyId;
    Long roomId;
    String visitorName;
    String visitorPhone;
    String visitorEmail;
    LocalDateTime preferredStart;
    String notes;
}
