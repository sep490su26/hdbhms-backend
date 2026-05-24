package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VisitRequestResponse {
    String visitorName;
    String visitorPhone;
    String visitorEmail;
    LocalDateTime preferredStart;
    LocalDateTime createdAt;
}
