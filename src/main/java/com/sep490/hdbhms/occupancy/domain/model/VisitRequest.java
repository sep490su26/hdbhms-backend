package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.VisitRequestSource;
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
    String propertyName;
    Long roomId;
    String roomCode;
    String roomName;
    String visitorName;
    String visitorPhone;
    String visitorEmail;
    LocalDateTime preferredStart;
    @Builder.Default
    VisitRequestStatus status = VisitRequestStatus.PENDING;
    VisitRequestSource source;
    String notes;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;

    public static VisitRequest create(
            Long propertyId,
            Long roomId,
            String visitorName,
            String visitorPhone,
            String visitorEmail,
            LocalDateTime preferredStart,
            VisitRequestSource source,
            String notes
    ) {
        return VisitRequest.builder()
                .propertyId(propertyId)
                .roomId(roomId)
                .visitorName(visitorName)
                .visitorPhone(visitorPhone)
                .visitorEmail(visitorEmail)
                .preferredStart(preferredStart)
                .status(VisitRequestStatus.PENDING)
                .source(source)
                .notes(notes)
                .build();
    }
}
