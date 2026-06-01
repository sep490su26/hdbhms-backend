package com.sep490.hdbhms.occupancy.domain.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Lead {
    Long id;
    Long propertyId;
    Long userId;
    LocalDate desiredMoveInDate;
    String note;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static Lead newLeadUser(Long propertyId, Long userId) {
        return Lead.builder()
                .propertyId(propertyId)
                .userId(userId)
                .build();
    }
}
