package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.VisitRequestStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.VisitRequestSource;
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
    VisitRequestSource source;
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
                .source(source)
                .notes(notes)
                .build();
    }
}
