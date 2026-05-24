package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.VisitRequestStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VisitRequest {
    Long id;
    Long propertyId;
    Long roomId;
    Long leadId;
    String visitorName;
    String visitorPhone;
    String visitorEmail;
    LocalDateTime preferredStart;
    LocalDateTime preferredEnd;
    VisitRequestStatus status;
    String notes;
    Long createdById;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static VisitRequest create(
            Long propertyId,
            Long roomId,
            Long leadId,
            String visitorName,
            String visitorPhone,
            String visitorEmail,
            LocalDateTime preferredStart,
            LocalDateTime preferredEnd,
            String notes,
            Long createdById
    ) {
        return VisitRequest.builder()
                .propertyId(propertyId)
                .roomId(roomId)
                .leadId(leadId)
                .visitorName(visitorName)
                .visitorPhone(visitorPhone)
                .visitorEmail(visitorEmail)
                .preferredStart(preferredStart)
                .preferredEnd(preferredEnd)
                .status(VisitRequestStatus.REQUESTED)
                .notes(notes)
                .createdById(createdById)
                .build();
    }
}