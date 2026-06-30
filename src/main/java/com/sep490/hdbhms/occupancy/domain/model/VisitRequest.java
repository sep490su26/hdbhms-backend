package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.valueObjects.VisitRequestStatus;
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
    String visitorName;
    String visitorPhone;
    String visitorEmail;
    LocalDateTime preferredStart;
    @Builder.Default
    VisitRequestStatus status = VisitRequestStatus.NOT_VIEWED;
    String notes;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Long deletedByUserId;
    LocalDateTime deletedAt;

    public static VisitRequest create(
            Long propertyId,
            Long roomId,
            String visitorName,
            String visitorPhone,
            String visitorEmail,
            LocalDateTime preferredStart,
            String notes
    ) {
        return VisitRequest.builder()
                .propertyId(propertyId)
                .roomId(roomId)
                .visitorName(visitorName)
                .visitorPhone(visitorPhone)
                .visitorEmail(visitorEmail)
                .preferredStart(preferredStart)
                .notes(notes)
                .build();
    }
}
